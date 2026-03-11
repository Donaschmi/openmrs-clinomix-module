package org.openmrs.module.clinomix.model;

import java.util.Date;

/**
 * Associates a FHIR QuestionnaireResponse ID with a {@link Study}.
 * New instances are created each time a response is added to a study,
 * which may happen after the study has already started.
 */
public class StudyResponse {

    private Integer id;
    private Study study;
    private String questionnaireResponseFhirId;
    private Date dateAdded;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Study getStudy() { return study; }
    public void setStudy(Study study) { this.study = study; }

    public String getQuestionnaireResponseFhirId() { return questionnaireResponseFhirId; }
    public void setQuestionnaireResponseFhirId(String questionnaireResponseFhirId) {
        this.questionnaireResponseFhirId = questionnaireResponseFhirId;
    }

    public Date getDateAdded() { return dateAdded; }
    public void setDateAdded(Date dateAdded) { this.dateAdded = dateAdded; }
}
