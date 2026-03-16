package org.openmrs.module.clinomx.api;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Lightweight FHIR REST client built on Spring {@link RestTemplate}.
 * Uses the HAPI FHIR parser (provided by the fhir2 module) for JSON
 * serialisation/deserialisation, so no hapi-fhir-client jar is needed.
 */
public class FhirRestClient {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private final String baseUrl;
    private final FhirContext fhirContext;

    public FhirRestClient(String baseUrl, FhirContext fhirContext) {
        this.baseUrl = baseUrl;
        this.fhirContext = fhirContext;
    }

    public <T extends IBaseResource> T read(Class<T> resourceType, String id) {
        String url = baseUrl + "/" + resourceType.getSimpleName() + "/" + id;
        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.GET, new HttpEntity<>(fhirHeaders()), String.class);
        return fhirContext.newJsonParser().parseResource(resourceType, response.getBody());
    }

    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T create(T resource) {
        String url = baseUrl + "/" + resource.fhirType();
        String json = fhirContext.newJsonParser().encodeResourceToString(resource);
        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.POST, new HttpEntity<>(json, fhirHeaders()), String.class);
        return (T) fhirContext.newJsonParser().parseResource(resource.getClass(), response.getBody());
    }

    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T update(T resource, String id) {
        String url = baseUrl + "/" + resource.fhirType() + "/" + id;
        String json = fhirContext.newJsonParser().encodeResourceToString(resource);
        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(json, fhirHeaders()), String.class);
        return (T) fhirContext.newJsonParser().parseResource(resource.getClass(), response.getBody());
    }

    public void delete(String resourceType, String id) {
        String url = baseUrl + "/" + resourceType + "/" + id;
        REST_TEMPLATE.exchange(url, HttpMethod.DELETE, new HttpEntity<>(fhirHeaders()), String.class);
    }

    public Bundle search(String resourceType, String queryString) {
        String url = baseUrl + "/" + resourceType
                + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.GET, new HttpEntity<>(fhirHeaders()), String.class);
        return fhirContext.newJsonParser().parseResource(Bundle.class, response.getBody());
    }

    public boolean exists(String resourceType, String id) {
        String url = baseUrl + "/" + resourceType + "/" + id;
        try {
            REST_TEMPLATE.exchange(url, HttpMethod.GET, new HttpEntity<>(fhirHeaders()), String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    private HttpHeaders fhirHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/fhir+json");
        headers.set("Accept", "application/fhir+json");
        return headers;
    }
}
