package org.openmrs.module.clinomx.model;

import java.util.Date;

/** Persistent record for a stored FHIR R4 QuestionnaireResponse. */
public class QuestionnaireResponseRecord {

    private Integer id;
    private String uuid;
    private String fhirId;
    private Integer questionnaireId;
    private String patientUuid;
    private String status;
    private Date authored;
    private String fhirJson;
    private Date dateCreated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getFhirId() { return fhirId; }
    public void setFhirId(String fhirId) { this.fhirId = fhirId; }

    public Integer getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(Integer questionnaireId) { this.questionnaireId = questionnaireId; }

    public String getPatientUuid() { return patientUuid; }
    public void setPatientUuid(String patientUuid) { this.patientUuid = patientUuid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getAuthored() { return authored; }
    public void setAuthored(Date authored) { this.authored = authored; }

    public String getFhirJson() { return fhirJson; }
    public void setFhirJson(String fhirJson) { this.fhirJson = fhirJson; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
