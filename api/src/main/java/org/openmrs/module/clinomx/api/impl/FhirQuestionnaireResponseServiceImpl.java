package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomx.api.FhirQuestionnaireResponseService;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireResponseRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link FhirQuestionnaireResponseService}.
 * Uses HAPI FHIR as a library for parsing and serialising FHIR JSON;
 * stores the result as a JSON blob in the OpenMRS database via {@link ClinomXDao}.
 * The {@code questionnaire_id} and {@code patient_uuid} columns are extracted
 * at write time to support efficient queries without JSON scanning.
 */
@Transactional
public class FhirQuestionnaireResponseServiceImpl extends BaseOpenmrsService
        implements FhirQuestionnaireResponseService {

    private FhirContext fhirContext;
    private ClinomXDao dao;

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public void setDao(ClinomXDao dao) {
        this.dao = dao;
    }

    @Override
    public QuestionnaireResponse getQuestionnaireResponseByUuid(String uuid) {
        QuestionnaireResponseRecord record = dao.getQuestionnaireResponseByUuid(uuid);
        return record != null ? toFhir(record) : null;
    }

    @Override
    public List<QuestionnaireResponse> getAllQuestionnaireResponses() {
        return dao.getAllQuestionnaireResponses().stream()
                .map(this::toFhir)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionnaireResponse> getResponsesByQuestionnaire(String questionnaireUuid) {
        return dao.getResponsesByQuestionnaire(questionnaireUuid).stream()
                .map(this::toFhir)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionnaireResponse> getResponsesByPatient(String patientUuid) {
        return dao.getResponsesByPatient(patientUuid).stream()
                .map(this::toFhir)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionnaireResponse> getResponsesByQuestionnaireAndPatient(String questionnaireUuid,
                                                                               String patientUuid) {
        return dao.getResponsesByQuestionnaireAndPatient(questionnaireUuid, patientUuid).stream()
                .map(this::toFhir)
                .collect(Collectors.toList());
    }

    @Override
    public QuestionnaireResponse createQuestionnaireResponse(QuestionnaireResponse response) {
        if (!response.hasId()) {
            response.setId(UUID.randomUUID().toString());
        }

        QuestionnaireResponseRecord record = new QuestionnaireResponseRecord();
        record.setUuid(UUID.randomUUID().toString());
        record.setFhirId(response.getIdElement().getIdPart());
        record.setQuestionnaireId(resolveQuestionnaireId(response.getQuestionnaire()));
        record.setPatientUuid(extractPatientUuid(response));
        record.setStatus(response.getStatus() != null ? response.getStatus().toCode() : null);
        record.setAuthored(response.getAuthored());
        record.setFhirJson(fhirContext.newJsonParser().encodeResourceToString(response));
        record.setDateCreated(new Date());

        dao.saveQuestionnaireResponse(record);
        return response;
    }

    @Override
    public QuestionnaireResponse updateQuestionnaireResponse(String uuid, QuestionnaireResponse response) {
        QuestionnaireResponseRecord record = dao.getQuestionnaireResponseByUuid(uuid);
        if (record == null) {
            throw new IllegalArgumentException("QuestionnaireResponse not found: " + uuid);
        }

        response.setId(record.getFhirId());
        record.setQuestionnaireId(resolveQuestionnaireId(response.getQuestionnaire()));
        record.setPatientUuid(extractPatientUuid(response));
        record.setStatus(response.getStatus() != null ? response.getStatus().toCode() : null);
        record.setAuthored(response.getAuthored());
        record.setFhirJson(fhirContext.newJsonParser().encodeResourceToString(response));

        dao.saveQuestionnaireResponse(record);
        return response;
    }

    @Override
    public void deleteQuestionnaireResponse(String uuid) {
        QuestionnaireResponseRecord record = dao.getQuestionnaireResponseByUuid(uuid);
        if (record != null) {
            dao.deleteQuestionnaireResponse(record);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Integer resolveQuestionnaireId(String questionnaireRef) {
        if (questionnaireRef == null) return null;
        String uuid = questionnaireRef.startsWith("Questionnaire/")
                ? questionnaireRef.substring("Questionnaire/".length())
                : questionnaireRef;
        org.openmrs.module.clinomx.model.QuestionnaireRecord q = dao.getQuestionnaireByUuid(uuid);
        return q != null ? q.getId() : null;
    }

    private String extractPatientUuid(QuestionnaireResponse response) {
        if (!response.hasSubject()) return null;
        String ref = response.getSubject().getReference();
        if (ref == null) return null;
        return ref.startsWith("Patient/") ? ref.substring("Patient/".length()) : ref;
    }

    private QuestionnaireResponse toFhir(QuestionnaireResponseRecord record) {
        QuestionnaireResponse r = fhirContext.newJsonParser()
                .parseResource(QuestionnaireResponse.class, record.getFhirJson());
        // Expose the DB uuid as the resource ID
        r.setId(record.getUuid());
        return r;
    }
}
