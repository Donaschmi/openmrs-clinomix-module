package org.openmrs.module.clinomx.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.clinomx.api.ClinomXService;
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
 * REST resource exposing basic ClinomX module info.
 *
 * Endpoint: GET /ws/rest/v1/clinom-x/info
 *
 * Replace or extend this with actual domain resources as needed.
 */
@Resource(name = RestConstants.VERSION_1 + "/clinom-x-module-info",
        supportedClass = ClinomXModuleInfo.class,
        supportedOpenmrsVersions = {"2.6.* - 9.*"})
public class ClinomXResource extends DelegatingCrudResource<ClinomXModuleInfo> {

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description.addProperty("info");
        }
        return description;
    }

    @Override
    public ClinomXModuleInfo getByUniqueId(String uniqueId) {
        String moduleInfo = Context.getService(ClinomXService.class).getModuleInfo();
        return new ClinomXModuleInfo(moduleInfo);
    }

    @Override
    public void delete(ClinomXModuleInfo delegate, String reason, RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException("Delete not supported for module info");
    }

    @Override
    public ClinomXModuleInfo newDelegate() {
        return new ClinomXModuleInfo();
    }

    @Override
    public ClinomXModuleInfo save(ClinomXModuleInfo delegate) {
        throw new UnsupportedOperationException("Save not supported for module info");
    }

    @Override
    public void purge(ClinomXModuleInfo delegate, RequestContext context) throws ResponseException {
        throw new UnsupportedOperationException("Purge not supported for module info");
    }
}
