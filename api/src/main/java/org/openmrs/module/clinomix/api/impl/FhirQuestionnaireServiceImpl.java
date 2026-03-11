package org.openmrs.module.clinomix.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomix.api.FhirClientFactory;
import org.openmrs.module.clinomix.api.FhirQuestionnaireService;
import org.openmrs.module.clinomix.dao.ClinomixDao;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link FhirQuestionnaireService}.
 * Delegates FHIR resource storage to an external HAPI FHIR JPA server whose
 * URL is configured via the OpenMRS global property {@code clinomix.fhirServerUrl}.
 *
 * Version history is persisted as JSON arrays in OpenMRS global properties
 * (key: {@code clinomix.history.<questionnaireId>}).
 */
@Transactional
public class FhirQuestionnaireServiceImpl extends BaseOpenmrsService implements FhirQuestionnaireService {

    static final String HISTORY_GP_PREFIX = "clinomix.history.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FhirContext fhirContext;
    private FhirClientFactory fhirClientFactory;
    private AdministrationService administrationService;
    private ClinomixDao dao;

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public void setFhirClientFactory(FhirClientFactory fhirClientFactory) {
        this.fhirClientFactory = fhirClientFactory;
    }

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public void setDao(ClinomixDao dao) {
        this.dao = dao;
    }

    // ── FHIR CRUD ─────────────────────────────────────────────────────────────

    @Override
    public List<Questionnaire> getQuestionnaires() {
        Bundle bundle = fhirClientFactory.getClient()
                .search()
                .forResource(Questionnaire.class)
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Questionnaire)
                .map(e -> (Questionnaire) e.getResource())
                .collect(Collectors.toList());
    }

    @Override
    public Questionnaire getQuestionnaireById(String id) {
        return fhirClientFactory.getClient()
                .read()
                .resource(Questionnaire.class)
                .withId(id)
                .execute();
    }

    @Override
    public IBundleProvider searchQuestionnaires(String title, Integer count, String sort) {
        IQuery<Bundle> query = fhirClientFactory.getClient()
                .search()
                .forResource(Questionnaire.class)
                .returnBundle(Bundle.class);

        if (StringUtils.isNotBlank(title)) {
            query = query.where(Questionnaire.TITLE.contains().value(title));
        }
        if (count != null) {
            query = query.count(count);
        }

        Bundle bundle = query.execute();
        List<IBaseResource> resources = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Questionnaire)
                .map(e -> (IBaseResource) e.getResource())
                .collect(Collectors.toList());

        return new SimpleBundleProvider(resources);
    }

    @Override
    public Questionnaire createQuestionnaire(Questionnaire questionnaire) {
        MethodOutcome outcome = fhirClientFactory.getClient()
                .create()
                .resource(questionnaire)
                .execute();

        return (Questionnaire) outcome.getResource();
    }

    @Override
    public Questionnaire updateQuestionnaire(String id, Questionnaire questionnaire) {
        questionnaire.setId(id);
        MethodOutcome outcome = fhirClientFactory.getClient()
                .update()
                .resource(questionnaire)
                .execute();

        return (Questionnaire) outcome.getResource();
    }

    @Override
    public void deleteQuestionnaire(String id) {
        fhirClientFactory.getClient()
                .delete()
                .resourceById("Questionnaire", id)
                .execute();
    }

    // ── Version history ────────────────────────────────────────────────────────

    @Override
    public List<String> getVersionHistory(String questionnaireId) {
        String stored = administrationService
                .getGlobalProperty(HISTORY_GP_PREFIX + questionnaireId);
        if (StringUtils.isBlank(stored)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(stored, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void archiveVersion(String questionnaireId, String questionnaireJson) {
        List<String> history = getVersionHistory(questionnaireId);
        history.add(questionnaireJson);
        persistHistory(questionnaireId, history);
    }

    @Override
    public void deleteArchivedVersion(String questionnaireId, int index) {
        List<String> history = getVersionHistory(questionnaireId);
        if (index < 0 || index >= history.size()) {
            throw new IndexOutOfBoundsException(
                    "History index " + index + " out of range (size=" + history.size() + ")");
        }
        history.remove(index);
        persistHistory(questionnaireId, history);
    }

    @Override
    public Questionnaire restoreVersion(String questionnaireId, int index) {
        List<String> history = getVersionHistory(questionnaireId);
        if (index < 0 || index >= history.size()) {
            throw new IndexOutOfBoundsException(
                    "History index " + index + " out of range (size=" + history.size() + ")");
        }
        String snapshotJson = history.get(index);
        Questionnaire snapshot = (Questionnaire) fhirContext.newJsonParser()
                .parseResource(snapshotJson);
        return updateQuestionnaire(questionnaireId, snapshot);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private void persistHistory(String questionnaireId, List<String> history) {
        try {
            String json = objectMapper.writeValueAsString(history);
            String key = HISTORY_GP_PREFIX + questionnaireId;
            GlobalProperty gp = administrationService.getGlobalPropertyObject(key);
            if (gp == null) {
                gp = new GlobalProperty(key, json, "Version history for questionnaire " + questionnaireId);
            } else {
                gp.setPropertyValue(json);
            }
            administrationService.saveGlobalProperty(gp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist version history for " + questionnaireId, e);
        }
    }
}
