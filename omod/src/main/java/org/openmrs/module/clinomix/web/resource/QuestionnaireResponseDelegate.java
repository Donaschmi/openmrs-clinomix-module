package org.openmrs.module.clinomix.web.resource;

/**
 * Delegate DTO wrapping a FHIR R4 QuestionnaireResponse for the OpenMRS REST layer.
 */
public class QuestionnaireResponseDelegate {

    private String uuid;
    private String status;
    private String questionnaire;
    private String subject;
    private String authored;
    /** Full FHIR R4 JSON — used as the body for create / update operations. */
    private String json;

    public QuestionnaireResponseDelegate() {}

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQuestionnaire() { return questionnaire; }
    public void setQuestionnaire(String questionnaire) { this.questionnaire = questionnaire; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getAuthored() { return authored; }
    public void setAuthored(String authored) { this.authored = authored; }

    public String getJson() { return json; }
    public void setJson(String json) { this.json = json; }
}
