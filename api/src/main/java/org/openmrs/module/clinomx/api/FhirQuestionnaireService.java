package org.openmrs.module.clinomx.api;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomx.ClinomXPrivileges;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for managing FHIR R4 Questionnaire resources.
 * Endpoints served: /ws/fhir2/R4/Questionnaire
 */
@Transactional
public interface FhirQuestionnaireService extends OpenmrsService {

    /**
     * GET /ws/fhir2/R4/Questionnaire
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<Questionnaire> getQuestionnaires();

    /**
     * GET /ws/fhir2/R4/Questionnaire/{id}
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    Questionnaire getQuestionnaireById(String id);

    /**
     * GET /ws/fhir2/R4/Questionnaire?_count=N&_sort=title
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    IBundleProvider searchQuestionnaires(String title, Integer count, String sort);

    /**
     * POST /ws/fhir2/R4/Questionnaire
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    Questionnaire createQuestionnaire(Questionnaire questionnaire);

    /**
     * PUT /ws/fhir2/R4/Questionnaire/{id}
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    Questionnaire updateQuestionnaire(String id, Questionnaire questionnaire);

    /**
     * DELETE /ws/fhir2/R4/Questionnaire/{id}
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    void deleteQuestionnaire(String id);

    // ── Version history ──────────────────────────────────────────────────────

    /**
     * GET /ws/rest/v1/clinom-x/questionnaire/{id}/history
     * Returns all archived versions of a questionnaire as a JSON list.
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<String> getVersionHistory(String questionnaireId);

    /**
     * POST /ws/rest/v1/clinom-x/questionnaire/{id}/history
     * Archives the current version of the questionnaire.
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    void archiveVersion(String questionnaireId, String questionnaireJson);

    /**
     * DELETE /ws/rest/v1/clinom-x/questionnaire/{id}/history/{index}
     * Removes an archived snapshot at the given index.
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    void deleteArchivedVersion(String questionnaireId, int index);

    /**
     * PUT /ws/rest/v1/clinom-x/questionnaire/{id}/restore/{index}
     * Restores the questionnaire to the archived snapshot at the given index.
     */
    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    Questionnaire restoreVersion(String questionnaireId, int index);
}
