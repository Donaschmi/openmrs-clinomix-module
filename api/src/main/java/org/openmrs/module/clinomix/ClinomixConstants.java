package org.openmrs.module.clinomix;

public final class ClinomixConstants {

    private ClinomixConstants() {}

    /**
     * OpenMRS Global Property: base URL of the HAPI FHIR REST server that the
     * Clinomix module delegates FHIR resource storage to.
     *
     * Default value registered in config.xml: {@code http://localhost:8082/fhir}
     *
     * Can be changed at runtime via Administration → Advanced Settings in OpenMRS.
     */
    public static final String GP_FHIR_SERVER_URL = "clinomix.fhirServerUrl";

    public static final String GP_FHIR_SERVER_URL_DEFAULT = "http://clinomix-hapi-fhir:8080/fhir";
}
