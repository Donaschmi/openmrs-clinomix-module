package org.openmrs.module.clinomx.api;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomx.ClinomXPrivileges;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service interface for managing FHIR R4 QuestionnaireResponse resources.
 * Endpoints served: /ws/fhir2/R4/QuestionnaireResponse
 */
@Transactional
public interface FhirQuestionnaireResponseService extends OpenmrsService {

    /**
     * GET /ws/fhir2/R4/QuestionnaireResponse/{id}
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    QuestionnaireResponse getQuestionnaireResponseById(String id);

    /**
     * GET /ws/fhir2/R4/QuestionnaireResponse?questionnaire={qId}&subject={patientRef}
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    IBundleProvider searchQuestionnaireResponses(String questionnaireRef, String subjectRef, Integer count);

    /**
     * POST /ws/fhir2/R4/QuestionnaireResponse
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    QuestionnaireResponse createQuestionnaireResponse(QuestionnaireResponse response);

    /**
     * PUT /ws/fhir2/R4/QuestionnaireResponse/{id}
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    QuestionnaireResponse updateQuestionnaireResponse(String id, QuestionnaireResponse response);

    /**
     * DELETE /ws/fhir2/R4/QuestionnaireResponse/{id}
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    void deleteQuestionnaireResponse(String id);
}
