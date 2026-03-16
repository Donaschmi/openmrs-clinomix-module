package org.openmrs.module.clinomix.api;

import ca.uhn.fhir.context.FhirContext;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.ClinomixConstants;

/**
 * Spring bean that vends a {@link FhirRestClient} configured to talk
 * to the external FHIR server whose URL is stored in the OpenMRS Global Property
 * {@value ClinomixConstants#GP_FHIR_SERVER_URL}.
 *
 * <p>The URL is re-read from the database on every call to {@link #getClient()},
 * so it can be updated in Administration → Advanced Settings without a module reload.</p>
 */
public class FhirClientFactory {

    private static final String DEFAULT_FHIR_SERVER_URL = "http://clinomix-hapi-fhir:8080/fhir";

    private final FhirContext fhirContext;

    public FhirClientFactory(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public FhirRestClient getClient() {
        String url = Context.getAdministrationService()
                .getGlobalProperty(ClinomixConstants.GP_FHIR_SERVER_URL, DEFAULT_FHIR_SERVER_URL);
        return new FhirRestClient(url, fhirContext);
    }

    public String getServerUrl() {
        return Context.getAdministrationService()
                .getGlobalProperty(ClinomixConstants.GP_FHIR_SERVER_URL, DEFAULT_FHIR_SERVER_URL);
    }
}
