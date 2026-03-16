package org.openmrs.module.clinomx.web.fhir;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomx.api.FhirQuestionnaireService;

/**
 * HAPI FHIR resource provider for the Questionnaire resource.
 *
 * Endpoints (served by the FHIR2 module's FhirRestServlet at /ws/fhir2/R4/):
 *   GET    Questionnaire/{id}
 *   GET    Questionnaire?_count=N&_sort=title
 *   POST   Questionnaire
 *   PUT    Questionnaire/{id}
 *   DELETE Questionnaire/{id}
 *
 * Registered as a Spring bean in omod/moduleApplicationContext.xml;
 * the FHIR2 RestServlet auto-discovers it via IResourceProvider type.
 */
public class QuestionnaireResourceProvider implements IResourceProvider {

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Questionnaire.class;
    }

    private FhirQuestionnaireService getService() {
        return Context.getService(FhirQuestionnaireService.class);
    }

    /**
     * GET /ws/fhir2/R4/Questionnaire/{id}
     */
    @Read
    public Questionnaire getById(@IdParam IdType id) {
        return getService().getQuestionnaireById(id.getIdPart());
    }

    /**
     * GET /ws/fhir2/R4/Questionnaire?title=...&_count=N&_sort=title
     */
    @Search
    public IBundleProvider search(
            @OptionalParam(name = Questionnaire.SP_TITLE) StringParam title,
            @OptionalParam(name = "_count") TokenParam count,
            @OptionalParam(name = "_sort") TokenParam sort) {

        String titleValue = title != null ? title.getValue() : null;
        Integer countValue = count != null ? Integer.parseInt(count.getValue()) : null;
        String sortValue = sort != null ? sort.getValue() : null;

        return getService().searchQuestionnaires(titleValue, countValue, sortValue);
    }

    /**
     * POST /ws/fhir2/R4/Questionnaire
     */
    @Create
    public MethodOutcome create(@ResourceParam Questionnaire questionnaire) {
        Questionnaire created = getService().createQuestionnaire(questionnaire);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(created);
        outcome.setCreated(true);
        return outcome;
    }

    /**
     * PUT /ws/fhir2/R4/Questionnaire/{id}
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Questionnaire questionnaire) {
        Questionnaire updated = getService().updateQuestionnaire(id.getIdPart(), questionnaire);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(updated);
        return outcome;
    }

    /**
     * DELETE /ws/fhir2/R4/Questionnaire/{id}
     */
    @Delete
    public void delete(@IdParam IdType id) {
        getService().deleteQuestionnaire(id.getIdPart());
    }
}
