package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomx.api.FhirQuestionnaireService;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link FhirQuestionnaireService}.
 * Uses HAPI FHIR as a library for parsing and serialising FHIR JSON;
 * stores the result as a JSON blob in the OpenMRS database via {@link ClinomXDao}.
 */
@Transactional
public class FhirQuestionnaireServiceImpl extends BaseOpenmrsService implements FhirQuestionnaireService {

    private static final Logger log = LoggerFactory.getLogger(FhirQuestionnaireServiceImpl.class);

    private FhirContext fhirContext;
    private ClinomXDao dao;

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public void setDao(ClinomXDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Questionnaire> getAllQuestionnaires() {
        return dao.getAllQuestionnaires().stream()
                .map(this::toFhir)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Questionnaire> searchQuestionnairesByTitle(String title) {
        return dao.searchQuestionnairesByTitle(title).stream()
                .map(this::toFhir)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Questionnaire getQuestionnaireByUuid(String uuid) {
        QuestionnaireRecord record = dao.getQuestionnaireByUuid(uuid);
        return record != null ? toFhir(record) : null;
    }

    @Override
    public Questionnaire createQuestionnaire(Questionnaire questionnaire) {
        if (!questionnaire.hasId()) {
            questionnaire.setId(UUID.randomUUID().toString());
        }

        QuestionnaireRecord record = new QuestionnaireRecord();
        record.setUuid(UUID.randomUUID().toString());
        record.setFhirId(questionnaire.getIdElement().getIdPart());
        record.setTitle(questionnaire.getTitle());
        record.setStatus(questionnaire.getStatus() != null ? questionnaire.getStatus().toCode() : null);
        record.setFhirJson(fhirContext.newJsonParser().encodeResourceToString(questionnaire));
        record.setDateCreated(new Date());

        dao.saveQuestionnaire(record);
        return questionnaire;
    }

    @Override
    public Questionnaire updateQuestionnaire(String uuid, Questionnaire questionnaire) {
        QuestionnaireRecord record = dao.getQuestionnaireByUuid(uuid);
        if (record == null) {
            throw new IllegalArgumentException("Questionnaire not found: " + uuid);
        }

        questionnaire.setId(record.getFhirId());
        record.setTitle(questionnaire.getTitle());
        record.setStatus(questionnaire.getStatus() != null ? questionnaire.getStatus().toCode() : null);
        record.setFhirJson(fhirContext.newJsonParser().encodeResourceToString(questionnaire));
        record.setDateChanged(new Date());

        dao.saveQuestionnaire(record);
        return questionnaire;
    }

    @Override
    public void deleteQuestionnaire(String uuid) {
        QuestionnaireRecord record = dao.getQuestionnaireByUuid(uuid);
        if (record != null) {
            dao.deleteQuestionnaire(record);
        }
    }

    private Questionnaire toFhir(QuestionnaireRecord record) {
        try {
            Questionnaire q = fhirContext.newJsonParser()
                    .parseResource(Questionnaire.class, record.getFhirJson());
            q.setId(record.getUuid());
            return q;
        } catch (DataFormatException e) {
            log.error("Questionnaire record {} contains invalid FHIR JSON and will be skipped: {}",
                    record.getUuid(), e.getMessage());
            return null;
        }
    }
}
