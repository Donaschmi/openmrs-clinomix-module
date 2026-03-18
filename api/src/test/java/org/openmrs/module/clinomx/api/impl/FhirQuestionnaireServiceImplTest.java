package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FhirQuestionnaireServiceImpl}.
 * ClinomXDao is mocked; FhirContext is used as a real library instance.
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirQuestionnaireServiceImplTest {

    private static final FhirContext FHIR_CTX = FhirContext.forR4Cached();

    @Mock private ClinomXDao dao;

    private FhirQuestionnaireServiceImpl service;

    @Before
    public void setUp() {
        service = new FhirQuestionnaireServiceImpl();
        service.setFhirContext(FHIR_CTX);
        service.setDao(dao);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static Questionnaire questionnaire(String id, String title) {
        Questionnaire q = new Questionnaire();
        q.setId(id);
        q.setTitle(title);
        q.setStatus(Enumerations.PublicationStatus.ACTIVE);
        return q;
    }

    private QuestionnaireRecord recordFor(String uuid, Questionnaire q) {
        QuestionnaireRecord r = new QuestionnaireRecord();
        r.setUuid(uuid);
        r.setFhirId(q.getIdElement().getIdPart());
        r.setTitle(q.getTitle());
        r.setStatus(q.getStatus().toCode());
        r.setFhirJson(FHIR_CTX.newJsonParser().encodeResourceToString(q));
        r.setDateCreated(new Date());
        return r;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getAllQuestionnaires
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getAllQuestionnaires_shouldReturnAllRecordsAsFhirObjects() {
        QuestionnaireRecord r1 = recordFor("uuid-1", questionnaire("fhir-1", "Patient Intake Form"));
        QuestionnaireRecord r2 = recordFor("uuid-2", questionnaire("fhir-2", "PHQ-9 Depression Screening"));
        when(dao.getAllQuestionnaires()).thenReturn(Arrays.asList(r1, r2));

        List<Questionnaire> result = service.getAllQuestionnaires();

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getTitle(), is("Patient Intake Form"));
        assertThat(result.get(1).getTitle(), is("PHQ-9 Depression Screening"));
    }

    @Test
    public void getAllQuestionnaires_shouldExposeDbUuidAsResourceId() {
        QuestionnaireRecord r = recordFor("uuid-1", questionnaire("fhir-1", "Form A"));
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(r));

        List<Questionnaire> result = service.getAllQuestionnaires();

        assertThat(result.get(0).getIdElement().getIdPart(), is("uuid-1"));
    }

    @Test
    public void getAllQuestionnaires_shouldReturnEmptyListWhenNoneStored() {
        when(dao.getAllQuestionnaires()).thenReturn(Collections.emptyList());

        List<Questionnaire> result = service.getAllQuestionnaires();

        assertThat(result, notNullValue());
        assertThat(result, hasSize(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // searchQuestionnairesByTitle
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void searchQuestionnairesByTitle_shouldReturnMatchingQuestionnaires() {
        QuestionnaireRecord r = recordFor("uuid-2", questionnaire("fhir-2", "PHQ-9 Depression Screening"));
        when(dao.searchQuestionnairesByTitle("PHQ")).thenReturn(Collections.singletonList(r));

        List<Questionnaire> result = service.searchQuestionnairesByTitle("PHQ");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getTitle(), containsString("PHQ-9"));
    }

    @Test
    public void searchQuestionnairesByTitle_shouldReturnEmptyListWhenNoMatch() {
        when(dao.searchQuestionnairesByTitle("nonexistent")).thenReturn(Collections.emptyList());

        List<Questionnaire> result = service.searchQuestionnairesByTitle("nonexistent");

        assertThat(result, hasSize(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getQuestionnaireByUuid
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getQuestionnaireByUuid_shouldReturnMatchingQuestionnaire() {
        QuestionnaireRecord r = recordFor("uuid-1", questionnaire("fhir-1", "Patient Intake Form"));
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(r);

        Questionnaire result = service.getQuestionnaireByUuid("uuid-1");

        assertThat(result, notNullValue());
        assertThat(result.getTitle(), is("Patient Intake Form"));
    }

    @Test
    public void getQuestionnaireByUuid_shouldReturnNullWhenNotFound() {
        when(dao.getQuestionnaireByUuid("missing")).thenReturn(null);

        Questionnaire result = service.getQuestionnaireByUuid("missing");

        assertThat(result, nullValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void createQuestionnaire_shouldPersistRecordWithExtractedFields() {
        Questionnaire input = questionnaire("fhir-new", "New Form");

        service.createQuestionnaire(input);

        ArgumentCaptor<QuestionnaireRecord> captor = ArgumentCaptor.forClass(QuestionnaireRecord.class);
        verify(dao).saveQuestionnaire(captor.capture());

        QuestionnaireRecord saved = captor.getValue();
        assertThat(saved.getTitle(), is("New Form"));
        assertThat(saved.getStatus(), is("active"));
        assertThat(saved.getFhirId(), is("fhir-new"));
        assertThat(saved.getUuid(), notNullValue());
        assertThat(saved.getFhirJson(), containsString("New Form"));
        assertThat(saved.getDateCreated(), notNullValue());
    }

    @Test
    public void createQuestionnaire_shouldGenerateFhirIdWhenNotSet() {
        Questionnaire input = questionnaire(null, "No-ID Form");

        service.createQuestionnaire(input);

        ArgumentCaptor<QuestionnaireRecord> captor = ArgumentCaptor.forClass(QuestionnaireRecord.class);
        verify(dao).saveQuestionnaire(captor.capture());

        assertThat(captor.getValue().getFhirId(), notNullValue());
    }

    @Test
    public void createQuestionnaire_shouldReturnInputQuestionnaire() {
        Questionnaire input = questionnaire("fhir-1", "Form");

        Questionnaire result = service.createQuestionnaire(input);

        assertThat(result, is(input));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void updateQuestionnaire_shouldUpdateFieldsAndPersist() {
        QuestionnaireRecord existing = recordFor("uuid-1", questionnaire("fhir-1", "Old Title"));
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(existing);

        Questionnaire update = questionnaire(null, "New Title");
        service.updateQuestionnaire("uuid-1", update);

        ArgumentCaptor<QuestionnaireRecord> captor = ArgumentCaptor.forClass(QuestionnaireRecord.class);
        verify(dao).saveQuestionnaire(captor.capture());

        QuestionnaireRecord saved = captor.getValue();
        assertThat(saved.getTitle(), is("New Title"));
        assertThat(saved.getDateChanged(), notNullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateQuestionnaire_shouldThrowWhenNotFound() {
        when(dao.getQuestionnaireByUuid("missing")).thenReturn(null);

        service.updateQuestionnaire("missing", questionnaire(null, "Title"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // deleteQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void deleteQuestionnaire_shouldDeleteExistingRecord() {
        QuestionnaireRecord r = recordFor("uuid-1", questionnaire("fhir-1", "Form"));
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(r);

        service.deleteQuestionnaire("uuid-1");

        verify(dao).deleteQuestionnaire(r);
    }

    @Test
    public void deleteQuestionnaire_shouldDoNothingWhenNotFound() {
        when(dao.getQuestionnaireByUuid("missing")).thenReturn(null);

        service.deleteQuestionnaire("missing");

        verify(dao, never()).deleteQuestionnaire(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Malformed fhirJson — graceful error handling
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getQuestionnaireByUuid_shouldReturnNullWhenFhirJsonIsNotValidJson() {
        QuestionnaireRecord r = new QuestionnaireRecord();
        r.setUuid("bad-uuid");
        r.setFhirJson("{ this is not valid JSON !!!");
        when(dao.getQuestionnaireByUuid("bad-uuid")).thenReturn(r);

        Questionnaire result = service.getQuestionnaireByUuid("bad-uuid");

        assertThat(result, nullValue());
    }

    @Test
    public void getQuestionnaireByUuid_shouldReturnNullWhenFhirJsonIsWrongResourceType() {
        String patientJson = "{\"resourceType\":\"Patient\",\"id\":\"p1\","
                + "\"name\":[{\"family\":\"Smith\"}]}";
        QuestionnaireRecord r = new QuestionnaireRecord();
        r.setUuid("wrong-type-uuid");
        r.setFhirJson(patientJson);
        when(dao.getQuestionnaireByUuid("wrong-type-uuid")).thenReturn(r);

        Questionnaire result = service.getQuestionnaireByUuid("wrong-type-uuid");

        assertThat(result, nullValue());
    }

    @Test
    public void getAllQuestionnaires_shouldSkipRecordsWithMalformedFhirJson() {
        QuestionnaireRecord good = recordFor("uuid-good", questionnaire("fhir-1", "Good Form"));
        QuestionnaireRecord bad  = new QuestionnaireRecord();
        bad.setUuid("uuid-bad");
        bad.setFhirJson("not json at all");
        when(dao.getAllQuestionnaires()).thenReturn(Arrays.asList(good, bad));

        List<Questionnaire> result = service.getAllQuestionnaires();

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getTitle(), is("Good Form"));
    }
}
