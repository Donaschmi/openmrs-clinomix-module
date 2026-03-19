package org.openmrs.module.clinomx.dao;

import org.openmrs.module.clinomx.model.QuestionnaireRecord;
import org.openmrs.module.clinomx.model.QuestionnaireResponseRecord;

import java.util.List;

/**
 * Database access interface for the ClinomX module.
 */
public interface ClinomXDao {

    // ── Questionnaire ─────────────────────────────────────────────────────────

    QuestionnaireRecord saveQuestionnaire(QuestionnaireRecord questionnaire);

    QuestionnaireRecord getQuestionnaireByUuid(String uuid);

    QuestionnaireRecord getQuestionnaireByFhirId(String fhirId);

    List<QuestionnaireRecord> getAllQuestionnaires();

    List<QuestionnaireRecord> searchQuestionnairesByTitle(String title);

    /** Case-insensitive substring search on the FHIR {@code name} field (standard FHIR search parameter). */
    List<QuestionnaireRecord> searchQuestionnairesByName(String name);

    /**
     * Returns all records whose canonical {@code url} exactly matches the given value.
     * This is the FHIR-compliant way to retrieve all versions of the same logical
     * questionnaire — equivalent to the FHIR {@code $versions} operation on a canonical resource.
     */
    List<QuestionnaireRecord> getQuestionnairesByUrl(String url);

    /**
     * Returns the record whose {@code url} and {@code version} match exactly, or {@code null}
     * if none exists. Used to enforce the FHIR canonical uniqueness rule: the combination of
     * {@code url} + {@code version} must be unique across all questionnaires.
     * <p>
     * Note: a null {@code version} is treated as a distinct value (not equivalent to any string).
     */
    QuestionnaireRecord getQuestionnaireByUrlAndVersion(String url, String version);

    void deleteQuestionnaire(QuestionnaireRecord questionnaire);

    // ── QuestionnaireResponse ─────────────────────────────────────────────────

    QuestionnaireResponseRecord saveQuestionnaireResponse(QuestionnaireResponseRecord response);

    QuestionnaireResponseRecord getQuestionnaireResponseByUuid(String uuid);

    List<QuestionnaireResponseRecord> getAllQuestionnaireResponses();

    List<QuestionnaireResponseRecord> getResponsesByQuestionnaire(String questionnaireUuid);

    List<QuestionnaireResponseRecord> getResponsesByPatient(String patientUuid);

    List<QuestionnaireResponseRecord> getResponsesByQuestionnaireAndPatient(String questionnaireUuid, String patientUuid);

    void deleteQuestionnaireResponse(QuestionnaireResponseRecord response);
}
