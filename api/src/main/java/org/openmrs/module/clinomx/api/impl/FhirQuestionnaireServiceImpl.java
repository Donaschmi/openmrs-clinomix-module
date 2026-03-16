package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
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
import org.openmrs.module.clinomx.api.FhirClientFactory;
import org.openmrs.module.clinomx.api.FhirQuestionnaireService;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link FhirQuestionnaireService}.
 * Delegates FHIR resource storage to an external HAPI FHIR JPA server via
 * {@link FhirClientFactory}, using Spring RestTemplate for HTTP transport.
 */
@Transactional
public class FhirQuestionnaireServiceImpl extends BaseOpenmrsService implements FhirQuestionnaireService {

    static final String HISTORY_GP_PREFIX = "clinom-x.history.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FhirContext fhirContext;
    private FhirClientFactory fhirClientFactory;
    private AdministrationService administrationService;
    private ClinomXDao dao;

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public void setFhirClientFactory(FhirClientFactory fhirClientFactory) {
        this.fhirClientFactory = fhirClientFactory;
    }

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public void setDao(ClinomXDao dao) {
        this.dao = dao;
    }

    // ── FHIR CRUD ─────────────────────────────────────────────────────────────

    @Override
    public List<Questionnaire> getQuestionnaires() {
        Bundle bundle = fhirClientFactory.getClient().search("Questionnaire", null);
        return bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Questionnaire)
                .map(e -> (Questionnaire) e.getResource())
                .collect(Collectors.toList());
    }

    @Override
    public Questionnaire getQuestionnaireById(String id) {
        return fhirClientFactory.getClient().read(Questionnaire.class, id);
    }

    @Override
    public IBundleProvider searchQuestionnaires(String title, Integer count, String sort) {
        StringBuilder params = new StringBuilder();
        if (StringUtils.isNotBlank(title)) {
            params.append("title:contains=").append(title);
        }
        if (count != null) {
            if (params.length() > 0) params.append("&");
            params.append("_count=").append(count);
        }

        Bundle bundle = fhirClientFactory.getClient()
                .search("Questionnaire", params.length() > 0 ? params.toString() : null);
        List<IBaseResource> resources = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Questionnaire)
                .map(e -> (IBaseResource) e.getResource())
                .collect(Collectors.toList());

        return new SimpleBundleProvider(resources);
    }

    @Override
    public Questionnaire createQuestionnaire(Questionnaire questionnaire) {
        return fhirClientFactory.getClient().create(questionnaire);
    }

    @Override
    public Questionnaire updateQuestionnaire(String id, Questionnaire questionnaire) {
        questionnaire.setId(id);
        return fhirClientFactory.getClient().update(questionnaire, id);
    }

    @Override
    public void deleteQuestionnaire(String id) {
        fhirClientFactory.getClient().delete("Questionnaire", id);
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
