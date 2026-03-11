package org.openmrs.module.clinomix.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.api.StudyService;
import org.openmrs.module.clinomix.model.Study;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.List;

/**
 * REST resource for {@link Study}.
 *
 * Endpoints:
 *   GET    /ws/rest/v1/clinomix/study           — list all studies
 *   GET    /ws/rest/v1/clinomix/study/{uuid}    — get by UUID
 *   POST   /ws/rest/v1/clinomix/study           — create
 *   PUT    /ws/rest/v1/clinomix/study/{uuid}    — update
 *   DELETE /ws/rest/v1/clinomix/study/{uuid}    — delete
 */
@Resource(name = RestConstants.VERSION_1 + "/clinomix/study",
        supportedClass = Study.class,
        supportedOpenmrsVersions = {"2.6.* - 9.*"})
public class StudyResource extends DelegatingCrudResource<Study> {

    private StudyService getService() {
        return Context.getService(StudyService.class);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("uuid");
        description.addProperty("name");
        description.addProperty("description");
        description.addProperty("status");
        description.addProperty("questionnaireIds");
        if (rep instanceof FullRepresentation) {
            description.addProperty("responses");
            description.addProperty("dateCreated");
            description.addProperty("dateChanged");
        }
        description.addSelfLink();
        return description;
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addRequiredProperty("name");
        description.addProperty("description");
        description.addProperty("status");
        description.addProperty("questionnaireIds");
        return description;
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("name");
        description.addProperty("description");
        description.addProperty("status");
        description.addProperty("questionnaireIds");
        return description;
    }

    @Override
    public Study getByUniqueId(String uuid) {
        return getService().getStudyByUuid(uuid);
    }

    @Override
    public Study save(Study study) {
        if (study.getId() == null) {
            return getService().createStudy(study);
        } else {
            return getService().updateStudy(study.getUuid(), study);
        }
    }

    @Override
    public void delete(Study study, String reason, RequestContext context) throws ResponseException {
        getService().deleteStudy(study.getUuid());
    }

    @Override
    public Study newDelegate() {
        return new Study();
    }

    @Override
    public void purge(Study study, RequestContext context) throws ResponseException {
        getService().deleteStudy(study.getUuid());
    }

    @Override
    protected NeedsPaging<Study> doGetAll(RequestContext context) throws ResponseException {
        List<Study> studies = getService().getAllStudies();
        return new NeedsPaging<>(studies, context);
    }
}
