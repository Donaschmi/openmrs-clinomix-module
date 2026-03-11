package org.openmrs.module.clinomix.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.api.ClinomixService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * REST resource exposing basic Clinomix module info.
 *
 * Endpoint: GET /ws/rest/v1/clinomix/info
 *
 * Replace or extend this with actual domain resources as needed.
 */
@Resource(name = RestConstants.VERSION_1 + "/clinomix-module-info",
        supportedClass = ClinomixModuleInfo.class,
        supportedOpenmrsVersions = {"2.6.* - 9.*"})
public class ClinomixResource extends DelegatingCrudResource<ClinomixModuleInfo> {

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description.addProperty("info");
        }
        return description;
    }

    @Override
    public ClinomixModuleInfo getByUniqueId(String uniqueId) {
        String moduleInfo = Context.getService(ClinomixService.class).getModuleInfo();
        return new ClinomixModuleInfo(moduleInfo);
    }

    @Override
    public void delete(ClinomixModuleInfo delegate, String reason, RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException("Delete not supported for module info");
    }

    @Override
    public ClinomixModuleInfo newDelegate() {
        return new ClinomixModuleInfo();
    }

    @Override
    public ClinomixModuleInfo save(ClinomixModuleInfo delegate) {
        throw new UnsupportedOperationException("Save not supported for module info");
    }

    @Override
    public void purge(ClinomixModuleInfo delegate, RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException("Purge not supported for module info");
    }
}
