package org.openmrs.module.clinomix.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.BaseOpenmrsObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A Study groups a set of FHIR Questionnaires and their associated
 * QuestionnaireResponses. New responses may be added after the study has started.
 *
 * Questionnaire FHIR IDs are stored as a JSON array in {@code questionnaireIdsJson}.
 * Response associations are in {@link StudyResponse} rows joined by {@code study_id}.
 */
public class Study extends BaseOpenmrsObject {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Integer studyId;
    private String name;
    private String description;
    private String status = StudyStatus.ACTIVE.name();
    private String questionnaireIdsJson = "[]";
    private Date dateCreated;
    private Date dateChanged;
    private List<StudyResponse> responses = new ArrayList<>();

    @Override
    public Integer getId() {
        return studyId;
    }

    @Override
    public void setId(Integer id) {
        this.studyId = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQuestionnaireIdsJson() { return questionnaireIdsJson; }
    public void setQuestionnaireIdsJson(String questionnaireIdsJson) {
        this.questionnaireIdsJson = questionnaireIdsJson;
    }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public Date getDateChanged() { return dateChanged; }
    public void setDateChanged(Date dateChanged) { this.dateChanged = dateChanged; }

    public List<StudyResponse> getResponses() { return responses; }
    public void setResponses(List<StudyResponse> responses) { this.responses = responses; }

    // ── Convenience helpers for questionnaire ID list ─────────────────────────

    public List<String> getQuestionnaireIds() {
        if (StringUtils.isBlank(questionnaireIdsJson)) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(questionnaireIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setQuestionnaireIds(List<String> ids) {
        try {
            this.questionnaireIdsJson = MAPPER.writeValueAsString(ids != null ? ids : new ArrayList<>());
        } catch (Exception e) {
            this.questionnaireIdsJson = "[]";
        }
    }
}
