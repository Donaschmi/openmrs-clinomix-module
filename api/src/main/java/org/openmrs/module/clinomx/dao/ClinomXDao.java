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
