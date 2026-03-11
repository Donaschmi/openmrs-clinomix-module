package org.openmrs.module.clinomix.api;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomix.ClinomixPrivileges;
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
    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    List<Questionnaire> getQuestionnaires();

    /**
     * GET /ws/fhir2/R4/Questionnaire/{id}
     */
    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    Questionnaire getQuestionnaireById(String id);

    /**
     * GET /ws/fhir2/R4/Questionnaire?_count=N&_sort=title
     */
    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    IBundleProvider searchQuestionnaires(String title, Integer count, String sort);

    /**
     * POST /ws/fhir2/R4/Questionnaire
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Questionnaire createQuestionnaire(Questionnaire questionnaire);

    /**
     * PUT /ws/fhir2/R4/Questionnaire/{id}
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Questionnaire updateQuestionnaire(String id, Questionnaire questionnaire);

    /**
     * DELETE /ws/fhir2/R4/Questionnaire/{id}
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    void deleteQuestionnaire(String id);

    // ── Version history ──────────────────────────────────────────────────────

    /**
     * GET /ws/rest/v1/clinomix/questionnaire/{id}/history
     * Returns all archived versions of a questionnaire as a JSON list.
     */
    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    List<String> getVersionHistory(String questionnaireId);

    /**
     * POST /ws/rest/v1/clinomix/questionnaire/{id}/history
     * Archives the current version of the questionnaire.
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    void archiveVersion(String questionnaireId, String questionnaireJson);

    /**
     * DELETE /ws/rest/v1/clinomix/questionnaire/{id}/history/{index}
     * Removes an archived snapshot at the given index.
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    void deleteArchivedVersion(String questionnaireId, int index);

    /**
     * PUT /ws/rest/v1/clinomix/questionnaire/{id}/restore/{index}
     * Restores the questionnaire to the archived snapshot at the given index.
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Questionnaire restoreVersion(String questionnaireId, int index);
}
