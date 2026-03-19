package org.openmrs.module.clinomx.web.resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.LenientErrorHandler;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomx.api.FhirQuestionnaireResponseService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST resource for FHIR R4 QuestionnaireResponse resources.
 *
 * Endpoints:
 *   GET    /ws/rest/v1/questionnaireresponse                              — list all
 *   GET    /ws/rest/v1/questionnaireresponse?questionnaire={uuid}        — by questionnaire
 *   GET    /ws/rest/v1/questionnaireresponse?patient={uuid}              — by patient
 *   GET    /ws/rest/v1/questionnaireresponse?questionnaire={q}&patient={p} — both filters
 *   GET    /ws/rest/v1/questionnaireresponse/{uuid}                      — get by uuid
 *   POST   /ws/rest/v1/questionnaireresponse                             — create (body: {"json":"<FHIR JSON>"})
 *   POST   /ws/rest/v1/questionnaireresponse/{uuid}                      — update (body: {"json":"<FHIR JSON>"})
 *   DELETE /ws/rest/v1/questionnaireresponse/{uuid}                      — delete
 */
@Resource(name = RestConstants.VERSION_1 + "/questionnaireresponse",
        supportedClass = QuestionnaireResponseDelegate.class,
        supportedOpenmrsVersions = {"2.6.* - 9.*"})
public class QuestionnaireResponseRestResource extends DelegatingCrudResource<QuestionnaireResponseDelegate> {

    private static final FhirContext FHIR_CTX = FhirContext.forR4Cached();

    private FhirQuestionnaireResponseService getService() {
        return Context.getService(FhirQuestionnaireResponseService.class);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription d = new DelegatingResourceDescription();
        d.addProperty("uuid");
        d.addProperty("status");
        d.addProperty("questionnaire");
        d.addProperty("subject");
        d.addProperty("authored");
        if (rep instanceof FullRepresentation) {
            d.addProperty("json");
        }
        d.addSelfLink();
        return d;
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription d = new DelegatingResourceDescription();
        d.addRequiredProperty("json");
        d.addProperty("questionnaireUuid"); // optional shorthand — inferred into FHIR JSON
        d.addProperty("patientUuid");       // optional shorthand — inferred into FHIR JSON
        return d;
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() {
        DelegatingResourceDescription d = new DelegatingResourceDescription();
        d.addRequiredProperty("json");
        d.addProperty("questionnaireUuid");
        d.addProperty("patientUuid");
        return d;
    }

    @Override
    public QuestionnaireResponseDelegate getByUniqueId(String uuid) {
        QuestionnaireResponse r = getService().getQuestionnaireResponseByUuid(uuid);
        return toDelegate(r);
    }

    @Override
    public QuestionnaireResponseDelegate save(QuestionnaireResponseDelegate delegate) {
        QuestionnaireResponse r = FHIR_CTX.newJsonParser()
                .setParserErrorHandler(new LenientErrorHandler(false))
                .parseResource(QuestionnaireResponse.class, delegate.getJson());

        // If the caller supplied a shorthand questionnaireUuid and the FHIR JSON does not
        // already contain a questionnaire reference, inject it automatically.
        if (delegate.getQuestionnaireUuid() != null && !delegate.getQuestionnaireUuid().isEmpty()
                && !r.hasQuestionnaire()) {
            r.setQuestionnaire("Questionnaire/" + delegate.getQuestionnaireUuid());
        }

        // Same for patientUuid → subject reference.
        if (delegate.getPatientUuid() != null && !delegate.getPatientUuid().isEmpty()
                && !r.hasSubject()) {
            r.setSubject(new Reference("Patient/" + delegate.getPatientUuid()));
        }

        QuestionnaireResponse saved;
        if (delegate.getUuid() != null && !delegate.getUuid().isEmpty()) {
            saved = getService().updateQuestionnaireResponse(delegate.getUuid(), r);
        } else {
            saved = getService().createQuestionnaireResponse(r);
        }
        return toDelegate(saved);
    }

    @Override
    public void delete(QuestionnaireResponseDelegate delegate, String reason, RequestContext context)
            throws ResponseException {
        getService().deleteQuestionnaireResponse(delegate.getUuid());
    }

    @Override
    public void purge(QuestionnaireResponseDelegate delegate, RequestContext context) throws ResponseException {
        getService().deleteQuestionnaireResponse(delegate.getUuid());
    }

    @Override
    public QuestionnaireResponseDelegate newDelegate() {
        return new QuestionnaireResponseDelegate();
    }

    @Override
    protected NeedsPaging<QuestionnaireResponseDelegate> doGetAll(RequestContext context) throws ResponseException {
        List<QuestionnaireResponseDelegate> all = getService().getAllQuestionnaireResponses().stream()
                .map(this::toDelegate)
                .collect(Collectors.toList());
        return new NeedsPaging<>(all, context);
    }

    @Override
    protected NeedsPaging<QuestionnaireResponseDelegate> doSearch(RequestContext context) {
        String questionnaireUuid = context.getRequest().getParameter("questionnaire");
        String patientUuid = context.getRequest().getParameter("patient");

        List<QuestionnaireResponse> results;
        if (questionnaireUuid != null && patientUuid != null) {
            results = getService().getResponsesByQuestionnaireAndPatient(questionnaireUuid, patientUuid);
        } else if (questionnaireUuid != null) {
            results = getService().getResponsesByQuestionnaire(questionnaireUuid);
        } else if (patientUuid != null) {
            results = getService().getResponsesByPatient(patientUuid);
        } else {
            results = getService().getAllQuestionnaireResponses();
        }

        return new NeedsPaging<>(results.stream().map(this::toDelegate).collect(Collectors.toList()), context);
    }

    private QuestionnaireResponseDelegate toDelegate(QuestionnaireResponse r) {
        if (r == null) return null;
        QuestionnaireResponseDelegate d = new QuestionnaireResponseDelegate();
        d.setUuid(r.getIdElement().getIdPart());
        d.setStatus(r.getStatus() != null ? r.getStatus().toCode() : null);
        d.setQuestionnaire(r.getQuestionnaire());
        d.setSubject(r.getSubject() != null ? r.getSubject().getReference() : null);
        d.setAuthored(r.getAuthoredElement() != null ? r.getAuthoredElement().getValueAsString() : null);
        d.setJson(FHIR_CTX.newJsonParser().encodeResourceToString(r));
        return d;
    }
}
