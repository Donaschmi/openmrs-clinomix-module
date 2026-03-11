package org.openmrs.module.clinomix.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomix.ClinomixPrivileges;
import org.openmrs.module.clinomix.model.Study;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Studies.
 *
 * A Study groups FHIR Questionnaires and their QuestionnaireResponses.
 * New responses can be added after a study has started.
 *
 * REST endpoints (via StudyResource and StudyController):
 *   GET/POST/PUT/DELETE  /ws/rest/v1/clinomix/study
 *   POST                 /ws/rest/v1/clinomix/study/{uuid}/response
 *   DELETE               /ws/rest/v1/clinomix/study/{uuid}/response/{responseId}
 *   GET                  /ws/rest/v1/clinomix/study/{uuid}/export
 */
@Transactional
public interface StudyService extends OpenmrsService {

    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Study createStudy(Study study);

    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    Study getStudyByUuid(String uuid);

    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    List<Study> getAllStudies();

    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    List<Study> getStudiesByStatus(String status);

    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Study updateStudy(String uuid, Study updated);

    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    void deleteStudy(String uuid);

    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Study addQuestionnaire(String studyUuid, String questionnaireFhirId);

    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Study removeQuestionnaire(String studyUuid, String questionnaireFhirId);

    /**
     * Associates a QuestionnaireResponse (by its FHIR ID) with a study.
     * May be called after the study has already started.
     *
     * @return the updated Study
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Study addResponse(String studyUuid, String questionnaireResponseFhirId);

    /**
     * Removes the StudyResponse link with the given internal DB id.
     */
    @Authorized(ClinomixPrivileges.MANAGE_CLINOMIX_DATA)
    Study removeResponse(String studyUuid, Integer studyResponseId);

    /**
     * Exports the full study as a FHIR R4 Bundle JSON string.
     * The bundle contains the study metadata plus all Questionnaire and
     * QuestionnaireResponse resources fetched from the HAPI FHIR server.
     */
    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    String exportStudy(String studyUuid);
}
