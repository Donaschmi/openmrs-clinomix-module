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
    /** FHIR canonical URL ({@code Questionnaire.url}). Stable across versions — the primary grouping key. */
    private String url;
    private String name;
    private String title;
    private String status;
    private String version;
    private String fhirJson;
    private Date dateCreated;
    private Date dateChanged;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getFhirId() { return fhirId; }
    public void setFhirId(String fhirId) { this.fhirId = fhirId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    /** Machine-readable label (FHIR {@code Questionnaire.name}). Valid FHIR search parameter, but NOT the version-grouping key — use {@link #url} for that. */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Semantic version string (e.g. {@code 1.2.3}), extracted from FHIR {@code Questionnaire.version}. */
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getFhirJson() { return fhirJson; }
    public void setFhirJson(String fhirJson) { this.fhirJson = fhirJson; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public Date getDateChanged() { return dateChanged; }
    public void setDateChanged(Date dateChanged) { this.dateChanged = dateChanged; }
}
