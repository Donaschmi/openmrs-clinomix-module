package org.openmrs.module.clinomix.web.resource;

/**
 * Delegate DTO wrapping a FHIR R4 Questionnaire for the OpenMRS REST layer.
 *
 * Flat fields are populated on read; on write (POST/PUT) the caller supplies
 * {@code json} with the full FHIR JSON and optionally {@code versionChoice}.
 */
public class QuestionnaireDelegate {

    private String uuid;
    private String title;
    private String status;
    private String version;
    private String description;
    private String date;
    private String publisher;
    /** Full FHIR R4 JSON — used as the body for create / update operations. */
    private String json;

    public QuestionnaireDelegate() {}

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getJson() { return json; }
    public void setJson(String json) { this.json = json; }
}
