package org.openmrs.module.clinomix.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomix.api.FhirClientFactory;
import org.openmrs.module.clinomix.api.StudyService;
import org.openmrs.module.clinomix.dao.StudyDao;
import org.openmrs.module.clinomix.model.Study;
import org.openmrs.module.clinomix.model.StudyResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link StudyService}.
 */
@Transactional
public class StudyServiceImpl extends BaseOpenmrsService implements StudyService {

    private StudyDao studyDao;
    private FhirClientFactory fhirClientFactory;
    private FhirContext fhirContext;

    public void setStudyDao(StudyDao studyDao) {
        this.studyDao = studyDao;
    }

    public void setFhirClientFactory(FhirClientFactory fhirClientFactory) {
        this.fhirClientFactory = fhirClientFactory;
    }

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public Study createStudy(Study study) {
        if (study.getUuid() == null) {
            study.setUuid(UUID.randomUUID().toString());
        }
        study.setDateCreated(new Date());
        return studyDao.saveStudy(study);
    }

    @Override
    @Transactional(readOnly = true)
    public Study getStudyByUuid(String uuid) {
        return studyDao.getStudyByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Study> getAllStudies() {
        return studyDao.getAllStudies();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Study> getStudiesByStatus(String status) {
        return studyDao.getStudiesByStatus(status);
    }

    @Override
    public Study updateStudy(String uuid, Study updated) {
        Study existing = require(uuid);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getQuestionnaireIdsJson() != null) {
            existing.setQuestionnaireIdsJson(updated.getQuestionnaireIdsJson());
        }
        existing.setDateChanged(new Date());
        return studyDao.saveStudy(existing);
    }

    @Override
    public void deleteStudy(String uuid) {
        studyDao.deleteStudy(require(uuid));
    }

    // ── Questionnaire management ───────────────────────────────────────────────

    @Override
    public Study addQuestionnaire(String studyUuid, String questionnaireFhirId) {
        Study study = require(studyUuid);
        List<String> ids = study.getQuestionnaireIds();
        if (!ids.contains(questionnaireFhirId)) {
            ids.add(questionnaireFhirId);
            study.setQuestionnaireIds(ids);
            study.setDateChanged(new Date());
            studyDao.saveStudy(study);
        }
        return study;
    }

    @Override
    public Study removeQuestionnaire(String studyUuid, String questionnaireFhirId) {
        Study study = require(studyUuid);
        List<String> ids = study.getQuestionnaireIds();
        if (ids.remove(questionnaireFhirId)) {
            study.setQuestionnaireIds(ids);
            study.setDateChanged(new Date());
            studyDao.saveStudy(study);
        }
        return study;
    }

    // ── Response management ────────────────────────────────────────────────────

    @Override
    public Study addResponse(String studyUuid, String questionnaireResponseFhirId) {
        Study study = require(studyUuid);
        StudyResponse sr = new StudyResponse();
        sr.setStudy(study);
        sr.setQuestionnaireResponseFhirId(questionnaireResponseFhirId);
        sr.setDateAdded(new Date());
        study.getResponses().add(sr);
        study.setDateChanged(new Date());
        studyDao.saveStudy(study);
        return study;
    }

    @Override
    public Study removeResponse(String studyUuid, Integer studyResponseId) {
        Study study = require(studyUuid);
        StudyResponse sr = studyDao.getResponseById(studyResponseId);
        if (sr == null || !sr.getStudy().getUuid().equals(studyUuid)) {
            throw new IllegalArgumentException(
                    "Response " + studyResponseId + " does not belong to study " + studyUuid);
        }
        study.getResponses().removeIf(r -> r.getId().equals(studyResponseId));
        study.setDateChanged(new Date());
        studyDao.saveStudy(study);
        return study;
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public String exportStudy(String studyUuid) {
        Study study = require(studyUuid);
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setId(study.getUuid());

        IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(false);

        // Fetch each Questionnaire from HAPI FHIR and add to bundle
        for (String qId : study.getQuestionnaireIds()) {
            try {
                Questionnaire q = fhirClientFactory.getClient()
                        .read()
                        .resource(Questionnaire.class)
                        .withId(qId)
                        .execute();
                bundle.addEntry().setResource(q);
            } catch (Exception e) {
                // Log and continue — a missing questionnaire should not abort the export
            }
        }

        // Fetch each QuestionnaireResponse from HAPI FHIR and add to bundle
        for (StudyResponse sr : study.getResponses()) {
            try {
                QuestionnaireResponse qr = fhirClientFactory.getClient()
                        .read()
                        .resource(QuestionnaireResponse.class)
                        .withId(sr.getQuestionnaireResponseFhirId())
                        .execute();
                bundle.addEntry().setResource(qr);
            } catch (Exception e) {
                // Log and continue
            }
        }

        return jsonParser.encodeResourceToString(bundle);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Study require(String uuid) {
        Study study = studyDao.getStudyByUuid(uuid);
        if (study == null) {
            throw new IllegalArgumentException("No study found with uuid: " + uuid);
        }
        return study;
    }
}
