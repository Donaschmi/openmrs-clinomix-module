package org.openmrs.module.clinomx.model;

import java.util.Date;

/**
 * Persistent entity representing a stored FHIR R4 Questionnaire.
 * The full FHIR JSON is stored in {@link #fhirJson}; the scalar fields
 * are extracted at save time to support efficient querying.
 */
public class QuestionnaireRecord {

    private Integer id;
    private String uuid;
    private String fhirId;
    private String title;
    private String status;
    private String fhirJson;
    private Date dateCreated;
    private Date dateChanged;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getFhirId() { return fhirId; }
    public void setFhirId(String fhirId) { this.fhirId = fhirId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFhirJson() { return fhirJson; }
    public void setFhirJson(String fhirJson) { this.fhirJson = fhirJson; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public Date getDateChanged() { return dateChanged; }
    public void setDateChanged(Date dateChanged) { this.dateChanged = dateChanged; }
}
