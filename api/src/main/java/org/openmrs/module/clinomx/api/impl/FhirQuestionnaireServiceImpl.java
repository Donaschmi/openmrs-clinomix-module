package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomx.api.FhirQuestionnaireService;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link FhirQuestionnaireService}.
 * Uses HAPI FHIR as a library for parsing and serialising FHIR JSON;
 * stores the result as a JSON blob in the OpenMRS database via {@link ClinomXDao}.
 */
@Transactional
public class FhirQuestionnaireServiceImpl extends BaseOpenmrsService implements FhirQuestionnaireService {

    private static final Logger log = LoggerFactory.getLogger(FhirQuestionnaireServiceImpl.class);

    /**
     * Compares two version strings (e.g. "1.2.3") by numeric semver order.
     * Null or missing versions sort before all real versions.
     */
    static final Comparator<String> SEMVER_ORDER = (a, b) -> {
        int[] pa = parseSemver(a);
        int[] pb = parseSemver(b);
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(pa[i], pb[i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    };

    private static int[] parseSemver(String v) {
        if (v == null || v.isEmpty()) return new int[]{0, 0, 0};
        String[] parts = v.split("\\.", 3);
        int[] result = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            try { result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", "")); }
            catch (NumberFormatException ignored) { result[i] = 0; }
        }
        return result;
    }

    private FhirContext fhirContext;
    private ClinomXDao dao;

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public void setDao(ClinomXDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Questionnaire> getAllQuestionnaires() {
        return dao.getAllQuestionnaires().stream()
                .map(this::toFhir)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Questionnaire> searchQuestionnairesByTitle(String title) {
        return dao.searchQuestionnairesByTitle(title).stream()
                .map(this::toFhir)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Questionnaire> searchQuestionnairesByName(String name) {
        return dao.searchQuestionnairesByName(name).stream()
                .map(this::toFhir)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Questionnaire> getVersionsByUrl(String url) {
        return dao.getQuestionnairesByUrl(url).stream()
                .map(this::toFhir)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(q -> q.getVersion(), SEMVER_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public Questionnaire getQuestionnaireByUuid(String uuid) {
        QuestionnaireRecord record = dao.getQuestionnaireByUuid(uuid);
        return record != null ? toFhir(record) : null;
    }

    /**
     * Enforces the FHIR canonical uniqueness rule: the combination of {@code url} + {@code version}
     * must be unique across all questionnaires.
     *
     * @param url           the canonical URL to check (no-op when null or blank)
     * @param version       the version to check (null is treated as a distinct value)
     * @param excludeUuid   the UUID of the record being updated, or null on create;
     *                      a match on this UUID is not a conflict (the record is replacing itself)
     * @throws APIException if a different record with the same url+version already exists
     */
    private void assertNoDuplicate(String url, String version, String excludeUuid) {
        if (url == null || url.isEmpty()) return;

        QuestionnaireRecord existing = dao.getQuestionnaireByUrlAndVersion(url, version);
        if (existing == null) return;

        if (existing.getUuid().equals(excludeUuid)) return;

        throw new APIException(String.format(
                "A Questionnaire with url='%s' and version='%s' already exists (uuid=%s). " +
                "Increment the version to create a new version of this questionnaire.",
                url, version != null ? version : "(none)", existing.getUuid()));
    }

    @Override
    public Questionnaire createQuestionnaire(Questionnaire questionnaire) {
        assertNoDuplicate(questionnaire.getUrl(), questionnaire.getVersion(), null);

        if (!questionnaire.hasId()) {
            questionnaire.setId(UUID.randomUUID().toString());
        }

        QuestionnaireRecord record = new QuestionnaireRecord();
        record.setUuid(UUID.randomUUID().toString());
        record.setFhirId(questionnaire.getIdElement().getIdPart());
        record.setUrl(questionnaire.getUrl());
        record.setName(questionnaire.getName());
        record.setTitle(questionnaire.getTitle());
        record.setStatus(questionnaire.getStatus() != null ? questionnaire.getStatus().toCode() : null);
        record.setVersion(questionnaire.getVersion());
        record.setFhirJson(fhirContext.newJsonParser().encodeResourceToString(questionnaire));
        record.setDateCreated(new Date());

        dao.saveQuestionnaire(record);
        return questionnaire;
    }

    @Override
    public Questionnaire updateQuestionnaire(String uuid, Questionnaire questionnaire) {
        QuestionnaireRecord record = dao.getQuestionnaireByUuid(uuid);
        if (record == null) {
            throw new IllegalArgumentException("Questionnaire not found: " + uuid);
        }

        // Pass the current record's UUID so updating a questionnaire in-place (same url+version)
        // is not treated as a conflict with itself.
        assertNoDuplicate(questionnaire.getUrl(), questionnaire.getVersion(), record.getUuid());

        questionnaire.setId(record.getFhirId());
        record.setUrl(questionnaire.getUrl());
        record.setName(questionnaire.getName());
        record.setTitle(questionnaire.getTitle());
        record.setStatus(questionnaire.getStatus() != null ? questionnaire.getStatus().toCode() : null);
        record.setVersion(questionnaire.getVersion());
        record.setFhirJson(fhirContext.newJsonParser().encodeResourceToString(questionnaire));
        record.setDateChanged(new Date());

        dao.saveQuestionnaire(record);
        return questionnaire;
    }

    @Override
    public void deleteQuestionnaire(String uuid) {
        QuestionnaireRecord record = dao.getQuestionnaireByUuid(uuid);
        if (record != null) {
            dao.deleteQuestionnaire(record);
        }
    }

    private Questionnaire toFhir(QuestionnaireRecord record) {
        try {
            Questionnaire q = fhirContext.newJsonParser()
                    .parseResource(Questionnaire.class, record.getFhirJson());
            q.setId(record.getUuid());
            return q;
        } catch (DataFormatException e) {
            log.error("Questionnaire record {} contains invalid FHIR JSON and will be skipped: {}",
                    record.getUuid(), e.getMessage());
            return null;
        }
    }
}
