package org.openmrs.module.clinomx;

public final class ClinomXConstants {

    private ClinomXConstants() {}

    /**
     * OpenMRS Global Property: base URL of the HAPI FHIR REST server that the
     * ClinomX module delegates FHIR resource storage to.
     *
     * Default value registered in config.xml: {@code http://localhost:8082/fhir}
     *
     * Can be changed at runtime via Administration → Advanced Settings in OpenMRS.
     */
    public static final String GP_FHIR_SERVER_URL = "clinom-x.fhirServerUrl";

    public static final String GP_FHIR_SERVER_URL_DEFAULT = "http://clinom-x-hapi-fhir:8080/fhir";
}
