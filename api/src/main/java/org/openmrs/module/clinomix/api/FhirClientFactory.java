package org.openmrs.module.clinomix.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.ClinomixConstants;

/**
 * Spring bean that vends a HAPI FHIR {@link IGenericClient} configured to talk
 * to the external FHIR server whose URL is stored in the OpenMRS Global Property
 * {@value ClinomixConstants#GP_FHIR_SERVER_URL}.
 *
 * <p>The URL is re-read from the database on every call to {@link #getClient()},
 * so it can be updated in Administration → Advanced Settings without a module
 * reload.</p>
 */
public class FhirClientFactory {

    private static final String DEFAULT_FHIR_SERVER_URL = "http://localhost:8082/fhir";

    private final FhirContext fhirContext;

    public FhirClientFactory(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * Returns a new {@link IGenericClient} pointing at the configured FHIR server.
     * Reads the current value of the global property each time so runtime changes
     * take effect immediately.
     */
    public IGenericClient getClient() {
        String url = Context.getAdministrationService()
                .getGlobalProperty(ClinomixConstants.GP_FHIR_SERVER_URL, DEFAULT_FHIR_SERVER_URL);
        return fhirContext.newRestfulGenericClient(url);
    }

    /**
     * Returns the currently configured FHIR server base URL.
     */
    public String getServerUrl() {
        return Context.getAdministrationService()
                .getGlobalProperty(ClinomixConstants.GP_FHIR_SERVER_URL, DEFAULT_FHIR_SERVER_URL);
    }
}
