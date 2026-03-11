package org.openmrs.module.clinomix.web.fhir;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.api.FhirQuestionnaireResponseService;

/**
 * HAPI FHIR resource provider for QuestionnaireResponse.
 *
 * Endpoints (served by the FHIR2 module's FhirRestServlet at /ws/fhir2/R4/):
 *   GET    QuestionnaireResponse/{id}
 *   GET    QuestionnaireResponse?questionnaire={qId}&subject={ref}
 *   POST   QuestionnaireResponse
 *   PUT    QuestionnaireResponse/{id}
 *   DELETE QuestionnaireResponse/{id}
 *
 * Registered as a Spring bean in omod/moduleApplicationContext.xml;
 * the FHIR2 RestServlet auto-discovers it via IResourceProvider type.
 */
public class QuestionnaireResponseResourceProvider implements IResourceProvider {

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return QuestionnaireResponse.class;
    }

    private FhirQuestionnaireResponseService getService() {
        return Context.getService(FhirQuestionnaireResponseService.class);
    }

    /**
     * GET /ws/fhir2/R4/QuestionnaireResponse/{id}
     */
    @Read
    public QuestionnaireResponse getById(@IdParam IdType id) {
        return getService().getQuestionnaireResponseById(id.getIdPart());
    }

    /**
     * GET /ws/fhir2/R4/QuestionnaireResponse?questionnaire=...&subject=...
     */
    @Search
    public IBundleProvider search(
            @OptionalParam(name = QuestionnaireResponse.SP_QUESTIONNAIRE) ReferenceParam questionnaire,
            @OptionalParam(name = QuestionnaireResponse.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = "_count") TokenParam count) {

        String questionnaireRef = questionnaire != null ? questionnaire.getValue() : null;
        String subjectRef = subject != null ? subject.getValue() : null;
        Integer countValue = count != null ? Integer.parseInt(count.getValue()) : null;

        return getService().searchQuestionnaireResponses(questionnaireRef, subjectRef, countValue);
    }

    /**
     * POST /ws/fhir2/R4/QuestionnaireResponse
     */
    @Create
    public MethodOutcome create(@ResourceParam QuestionnaireResponse response) {
        QuestionnaireResponse created = getService().createQuestionnaireResponse(response);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(created);
        outcome.setCreated(true);
        return outcome;
    }

    /**
     * PUT /ws/fhir2/R4/QuestionnaireResponse/{id}
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam QuestionnaireResponse response) {
        QuestionnaireResponse updated = getService().updateQuestionnaireResponse(id.getIdPart(), response);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(updated);
        return outcome;
    }

    /**
     * DELETE /ws/fhir2/R4/QuestionnaireResponse/{id}
     */
    @Delete
    public void delete(@IdParam IdType id) {
        getService().deleteQuestionnaireResponse(id.getIdPart());
    }
}
