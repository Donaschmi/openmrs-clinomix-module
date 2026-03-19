package org.openmrs.module.clinomx.api;

import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomx.ClinomXPrivileges;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for managing FHIR R4 Questionnaire resources.
 * Resources are stored as JSON blobs in the OpenMRS database.
 *
 * Endpoints served: /ws/rest/v1/questionnaire
 */
@Transactional
public interface FhirQuestionnaireService extends OpenmrsService {

    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<Questionnaire> getAllQuestionnaires();

    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<Questionnaire> searchQuestionnairesByTitle(String title);

    /** Case-insensitive substring search on the FHIR {@code name} field (standard FHIR search parameter). */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<Questionnaire> searchQuestionnairesByName(String name);

    /**
     * Returns all versions of a questionnaire identified by its FHIR canonical URL
     * ({@code Questionnaire.url}), ordered from oldest to newest (ascending semver).
     * <p>
     * This is the FHIR-compliant equivalent of the {@code $versions} operation:
     * {@code url} is the stable identity shared across all versions of the same
     * logical questionnaire, as defined in the FHIR R4 spec.
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    List<Questionnaire> getVersionsByUrl(String url);

    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    Questionnaire getQuestionnaireByUuid(String uuid);

    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    Questionnaire createQuestionnaire(Questionnaire questionnaire);

    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    Questionnaire updateQuestionnaire(String uuid, Questionnaire questionnaire);

    @Authorized(ClinomXPrivileges.MANAGE_CLINOM_X_DATA)
    void deleteQuestionnaire(String uuid);
}
