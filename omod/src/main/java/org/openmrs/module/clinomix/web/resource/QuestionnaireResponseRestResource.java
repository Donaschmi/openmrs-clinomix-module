package org.openmrs.module.clinomix.web.resource;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.api.FhirQuestionnaireResponseService;
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
 *   GET    /ws/rest/v1/questionnaireresponse            — list all
 *   GET    /ws/rest/v1/questionnaireresponse/{id}       — get by id
 *   POST   /ws/rest/v1/questionnaireresponse            — create (body: {"json":"<FHIR JSON>"})
 *   POST   /ws/rest/v1/questionnaireresponse/{id}       — update (body: {"json":"<FHIR JSON>"})
 *   DELETE /ws/rest/v1/questionnaireresponse/{id}       — delete
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
        return d;
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() {
        DelegatingResourceDescription d = new DelegatingResourceDescription();
        d.addRequiredProperty("json");
        return d;
    }

    @Override
    public QuestionnaireResponseDelegate getByUniqueId(String id) {
        QuestionnaireResponse r = getService().getQuestionnaireResponseById(id);
        return toDelegate(r);
    }

    @Override
    public QuestionnaireResponseDelegate save(QuestionnaireResponseDelegate delegate) {
        QuestionnaireResponse r = FHIR_CTX.newJsonParser()
                .parseResource(QuestionnaireResponse.class, delegate.getJson());
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
        List<QuestionnaireResponseDelegate> all = getService()
                .searchQuestionnaireResponses(null, null, null)
                .getResources(0, Integer.MAX_VALUE).stream()
                .filter(r -> r instanceof QuestionnaireResponse)
                .map(r -> toDelegate((QuestionnaireResponse) r))
                .collect(Collectors.toList());
        return new NeedsPaging<>(all, context);
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
