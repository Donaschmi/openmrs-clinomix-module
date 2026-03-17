package org.openmrs.module.clinomx.web.resource;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomx.api.FhirQuestionnaireService;
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
 * REST resource for FHIR R4 Questionnaire resources.
 *
 * Endpoints:
 *   GET    /ws/rest/v1/questionnaire              — list all
 *   GET    /ws/rest/v1/questionnaire?title=X      — search by title
 *   GET    /ws/rest/v1/questionnaire/{uuid}       — get by uuid
 *   POST   /ws/rest/v1/questionnaire              — create  (body: {"json":"<FHIR JSON>"})
 *   POST   /ws/rest/v1/questionnaire/{uuid}       — update  (body: {"json":"<FHIR JSON>"})
 *   DELETE /ws/rest/v1/questionnaire/{uuid}       — delete
 */
@Resource(name = RestConstants.VERSION_1 + "/questionnaire",
        supportedClass = QuestionnaireDelegate.class,
        supportedOpenmrsVersions = {"2.6.* - 9.*"})
public class QuestionnaireRestResource extends DelegatingCrudResource<QuestionnaireDelegate> {

    private static final FhirContext FHIR_CTX = FhirContext.forR4Cached();

    private FhirQuestionnaireService getService() {
        return Context.getService(FhirQuestionnaireService.class);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription d = new DelegatingResourceDescription();
        d.addProperty("uuid");
        d.addProperty("title");
        d.addProperty("status");
        d.addProperty("version");
        d.addProperty("description");
        d.addProperty("date");
        d.addProperty("publisher");
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
    public QuestionnaireDelegate getByUniqueId(String uuid) {
        Questionnaire q = getService().getQuestionnaireByUuid(uuid);
        return toDelegate(q);
    }

    @Override
    public QuestionnaireDelegate save(QuestionnaireDelegate delegate) {
        Questionnaire q = FHIR_CTX.newJsonParser().parseResource(Questionnaire.class, delegate.getJson());
        Questionnaire saved;
        if (delegate.getUuid() != null && !delegate.getUuid().isEmpty()) {
            saved = getService().updateQuestionnaire(delegate.getUuid(), q);
        } else {
            saved = getService().createQuestionnaire(q);
        }
        return toDelegate(saved);
    }

    @Override
    public void delete(QuestionnaireDelegate delegate, String reason, RequestContext context) throws ResponseException {
        getService().deleteQuestionnaire(delegate.getUuid());
    }

    @Override
    public void purge(QuestionnaireDelegate delegate, RequestContext context) throws ResponseException {
        getService().deleteQuestionnaire(delegate.getUuid());
    }

    @Override
    public QuestionnaireDelegate newDelegate() {
        return new QuestionnaireDelegate();
    }

    @Override
    protected NeedsPaging<QuestionnaireDelegate> doGetAll(RequestContext context) throws ResponseException {
        List<QuestionnaireDelegate> all = getService().getAllQuestionnaires().stream()
                .map(this::toDelegate)
                .collect(Collectors.toList());
        return new NeedsPaging<>(all, context);
    }

    @Override
    protected NeedsPaging<QuestionnaireDelegate> doSearch(RequestContext context) {
        String title = context.getRequest().getParameter("title");
        List<QuestionnaireDelegate> results;
        if (title != null && !title.isEmpty()) {
            results = getService().searchQuestionnairesByTitle(title).stream()
                    .map(this::toDelegate)
                    .collect(Collectors.toList());
        } else {
            results = getService().getAllQuestionnaires().stream()
                    .map(this::toDelegate)
                    .collect(Collectors.toList());
        }
        return new NeedsPaging<>(results, context);
    }

    private QuestionnaireDelegate toDelegate(Questionnaire q) {
        if (q == null) return null;
        QuestionnaireDelegate d = new QuestionnaireDelegate();
        d.setUuid(q.getIdElement().getIdPart());
        d.setTitle(q.getTitle());
        d.setStatus(q.getStatus() != null ? q.getStatus().toCode() : null);
        d.setVersion(q.getVersion());
        d.setDescription(q.getDescription());
        d.setDate(q.getDateElement() != null ? q.getDateElement().getValueAsString() : null);
        d.setPublisher(q.getPublisher());
        d.setJson(FHIR_CTX.newJsonParser().encodeResourceToString(q));
        return d;
    }
}
