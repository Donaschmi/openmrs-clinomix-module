package org.openmrs.module.clinomx.api;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomx.ClinomXPrivileges;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for managing FHIR R4 QuestionnaireResponse resources.
 * Resources are stored as JSON blobs in the OpenMRS database.
 *
 * Endpoints served: /ws/rest/v1/questionnaireresponse
 */
@Transactional
public interface FhirQuestionnaireResponseService extends OpenmrsService {

    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    QuestionnaireResponse getQuestionnaireResponseByUuid(String uuid);

    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<QuestionnaireResponse> getAllQuestionnaireResponses();

    /** All responses submitted for a given questionnaire (by questionnaire UUID). */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<QuestionnaireResponse> getResponsesByQuestionnaire(String questionnaireUuid);

    /** All responses submitted by a given patient (by patient UUID). */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<QuestionnaireResponse> getResponsesByPatient(String patientUuid);

    /** Responses filtered by both questionnaire and patient. */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<QuestionnaireResponse> getResponsesByQuestionnaireAndPatient(String questionnaireUuid, String patientUuid);

    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    QuestionnaireResponse createQuestionnaireResponse(QuestionnaireResponse response);

    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    QuestionnaireResponse updateQuestionnaireResponse(String uuid, QuestionnaireResponse response);

    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    void deleteQuestionnaireResponse(String uuid);
}
