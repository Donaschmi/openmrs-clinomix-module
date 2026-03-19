package org.openmrs.module.clinomx.web.resource;

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

    /**
     * Optional shorthand for create / update: the OpenMRS UUID of the Questionnaire this
     * response belongs to.  When present, the REST resource automatically injects
     * {@code "questionnaire": "Questionnaire/{questionnaireUuid}"} into the parsed FHIR
     * resource before passing it to the service, so the frontend does not have to embed
     * the reference inside the raw JSON body.
     * If the FHIR JSON already contains a {@code questionnaire} reference it takes
     * precedence and this field is ignored.
     */
    private String questionnaireUuid;

    /**
     * Optional shorthand for create / update: the OpenMRS UUID of the patient who filled
     * out this response.  When present the REST resource injects
     * {@code "subject": {"reference": "Patient/{patientUuid}"}} into the parsed FHIR
     * resource.  Ignored when the FHIR JSON already contains a {@code subject} reference.
     */
    private String patientUuid;

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

    public String getQuestionnaireUuid() { return questionnaireUuid; }
    public void setQuestionnaireUuid(String questionnaireUuid) { this.questionnaireUuid = questionnaireUuid; }

    public String getPatientUuid() { return patientUuid; }
    public void setPatientUuid(String patientUuid) { this.patientUuid = patientUuid; }
}
