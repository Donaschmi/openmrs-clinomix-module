package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireRecord;
import org.openmrs.module.clinomx.model.QuestionnaireResponseRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FhirQuestionnaireResponseServiceImpl}.
 * ClinomXDao is mocked; FhirContext is used as a real library instance.
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirQuestionnaireResponseServiceImplTest {

    private static final FhirContext FHIR_CTX = FhirContext.forR4Cached();

    @Mock private ClinomXDao dao;

    private FhirQuestionnaireResponseServiceImpl service;

    @Before
    public void setUp() {
        service = new FhirQuestionnaireResponseServiceImpl();
        service.setFhirContext(FHIR_CTX);
        service.setDao(dao);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private QuestionnaireResponseRecord makeRecord(String uuid, String patientUuid, String status) {
        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId("fhir-" + uuid);
        qr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.fromCode(status));
        qr.setSubject(new Reference("Patient/" + patientUuid));

        QuestionnaireResponseRecord r = new QuestionnaireResponseRecord();
        r.setId(1);
        r.setUuid(uuid);
        r.setFhirId("fhir-" + uuid);
        r.setPatientUuid(patientUuid);
        r.setQuestionnaireId(10);
        r.setStatus(status);
        r.setFhirJson(FHIR_CTX.newJsonParser().encodeResourceToString(qr));
        r.setDateCreated(new Date());
        return r;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getAllQuestionnaireResponses
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getAllQuestionnaireResponses_shouldReturnAllRecordsAsFhirObjects() {
        when(dao.getAllQuestionnaireResponses()).thenReturn(Arrays.asList(
                makeRecord("uuid-1", "patient-1", "completed"),
                makeRecord("uuid-2", "patient-2", "in-progress")));

        List<QuestionnaireResponse> result = service.getAllQuestionnaireResponses();

        assertThat(result, hasSize(2));
        verify(dao).getAllQuestionnaireResponses();
    }

    @Test
    public void getAllQuestionnaireResponses_shouldExposeDbUuidAsResourceId() {
        when(dao.getAllQuestionnaireResponses())
                .thenReturn(Collections.singletonList(makeRecord("my-uuid", "p1", "completed")));

        List<QuestionnaireResponse> result = service.getAllQuestionnaireResponses();

        assertThat(result.get(0).getIdElement().getIdPart(), is("my-uuid"));
    }

    @Test
    public void getAllQuestionnaireResponses_shouldReturnEmptyListWhenNoneStored() {
        when(dao.getAllQuestionnaireResponses()).thenReturn(Collections.emptyList());

        List<QuestionnaireResponse> result = service.getAllQuestionnaireResponses();

        assertThat(result, empty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getQuestionnaireResponseByUuid
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getQuestionnaireResponseByUuid_shouldReturnMatchingResponse() {
        when(dao.getQuestionnaireResponseByUuid("target-uuid"))
                .thenReturn(makeRecord("target-uuid", "patient-1", "completed"));

        QuestionnaireResponse result = service.getQuestionnaireResponseByUuid("target-uuid");

        assertThat(result, notNullValue());
        assertThat(result.getIdElement().getIdPart(), is("target-uuid"));
    }

    @Test
    public void getQuestionnaireResponseByUuid_shouldReturnNullWhenNotFound() {
        when(dao.getQuestionnaireResponseByUuid("missing")).thenReturn(null);

        QuestionnaireResponse result = service.getQuestionnaireResponseByUuid("missing");

        assertThat(result, nullValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getResponsesByQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getResponsesByQuestionnaire_shouldDelegateToDao() {
        when(dao.getResponsesByQuestionnaire("q-uuid"))
                .thenReturn(Collections.singletonList(makeRecord("r1", "p1", "completed")));

        List<QuestionnaireResponse> result = service.getResponsesByQuestionnaire("q-uuid");

        assertThat(result, hasSize(1));
        verify(dao).getResponsesByQuestionnaire("q-uuid");
    }

    @Test
    public void getResponsesByQuestionnaire_shouldReturnEmptyWhenNoneFound() {
        when(dao.getResponsesByQuestionnaire("q-uuid")).thenReturn(Collections.emptyList());

        List<QuestionnaireResponse> result = service.getResponsesByQuestionnaire("q-uuid");

        assertThat(result, empty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getResponsesByPatient
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getResponsesByPatient_shouldDelegateToDao() {
        when(dao.getResponsesByPatient("patient-uuid"))
                .thenReturn(Collections.singletonList(makeRecord("r1", "patient-uuid", "completed")));

        List<QuestionnaireResponse> result = service.getResponsesByPatient("patient-uuid");

        assertThat(result, hasSize(1));
        verify(dao).getResponsesByPatient("patient-uuid");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getResponsesByQuestionnaireAndPatient
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getResponsesByQuestionnaireAndPatient_shouldDelegateToDao() {
        when(dao.getResponsesByQuestionnaireAndPatient("q-uuid", "p-uuid"))
                .thenReturn(Collections.singletonList(makeRecord("r1", "p-uuid", "completed")));

        List<QuestionnaireResponse> result =
                service.getResponsesByQuestionnaireAndPatient("q-uuid", "p-uuid");

        assertThat(result, hasSize(1));
        verify(dao).getResponsesByQuestionnaireAndPatient("q-uuid", "p-uuid");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createQuestionnaireResponse
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void createQuestionnaireResponse_shouldPersistRecordWithExtractedFields() {
        QuestionnaireResponse input = new QuestionnaireResponse();
        input.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        input.setSubject(new Reference("Patient/patient-uuid"));
        input.setQuestionnaire("Questionnaire/q-uuid");

        QuestionnaireRecord qRec = new QuestionnaireRecord();
        qRec.setId(42);
        when(dao.getQuestionnaireByUuid("q-uuid")).thenReturn(qRec);

        service.createQuestionnaireResponse(input);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());

        QuestionnaireResponseRecord saved = captor.getValue();
        assertThat(saved.getStatus(), is("completed"));
        assertThat(saved.getPatientUuid(), is("patient-uuid"));
        assertThat(saved.getQuestionnaireId(), is(42));
        assertThat(saved.getUuid(), notNullValue());
        assertThat(saved.getFhirJson(), containsString("completed"));
        assertThat(saved.getDateCreated(), notNullValue());
    }

    @Test
    public void createQuestionnaireResponse_shouldGenerateFhirIdWhenNotSet() {
        QuestionnaireResponse input = new QuestionnaireResponse();

        service.createQuestionnaireResponse(input);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());

        assertThat(captor.getValue().getFhirId(), notNullValue());
    }

    @Test
    public void createQuestionnaireResponse_shouldReturnInputResponse() {
        QuestionnaireResponse input = new QuestionnaireResponse();
        input.setId("fhir-id");

        QuestionnaireResponse result = service.createQuestionnaireResponse(input);

        assertThat(result, is(input));
    }

    @Test
    public void createQuestionnaireResponse_shouldStripPatientPrefixFromSubject() {
        QuestionnaireResponse input = new QuestionnaireResponse();
        input.setSubject(new Reference("Patient/stripped-uuid"));

        service.createQuestionnaireResponse(input);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());

        assertThat(captor.getValue().getPatientUuid(), is("stripped-uuid"));
    }

    @Test
    public void createQuestionnaireResponse_shouldStripQuestionnairePrefix() {
        QuestionnaireResponse input = new QuestionnaireResponse();
        input.setQuestionnaire("Questionnaire/q-with-prefix");

        QuestionnaireRecord qRec = new QuestionnaireRecord();
        qRec.setId(7);
        when(dao.getQuestionnaireByUuid("q-with-prefix")).thenReturn(qRec);

        service.createQuestionnaireResponse(input);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());
        assertThat(captor.getValue().getQuestionnaireId(), is(7));
    }

    @Test
    public void createQuestionnaireResponse_shouldSetNullQuestionnaireIdWhenRefNotFound() {
        QuestionnaireResponse input = new QuestionnaireResponse();
        input.setQuestionnaire("Questionnaire/nonexistent");
        when(dao.getQuestionnaireByUuid("nonexistent")).thenReturn(null);

        service.createQuestionnaireResponse(input);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());
        assertThat(captor.getValue().getQuestionnaireId(), nullValue());
    }

    @Test
    public void createQuestionnaireResponse_shouldSetNullPatientUuidWhenNoSubject() {
        QuestionnaireResponse input = new QuestionnaireResponse();

        service.createQuestionnaireResponse(input);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());
        assertThat(captor.getValue().getPatientUuid(), nullValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateQuestionnaireResponse
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void updateQuestionnaireResponse_shouldUpdateFieldsAndPersist() {
        QuestionnaireResponseRecord existing = makeRecord("existing-uuid", "old-patient", "in-progress");
        when(dao.getQuestionnaireResponseByUuid("existing-uuid")).thenReturn(existing);

        QuestionnaireResponse update = new QuestionnaireResponse();
        update.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        update.setSubject(new Reference("Patient/new-patient"));

        service.updateQuestionnaireResponse("existing-uuid", update);

        ArgumentCaptor<QuestionnaireResponseRecord> captor =
                ArgumentCaptor.forClass(QuestionnaireResponseRecord.class);
        verify(dao).saveQuestionnaireResponse(captor.capture());

        QuestionnaireResponseRecord saved = captor.getValue();
        assertThat(saved.getStatus(), is("completed"));
        assertThat(saved.getPatientUuid(), is("new-patient"));
        assertThat(saved.getFhirJson(), notNullValue());
    }

    @Test
    public void updateQuestionnaireResponse_shouldRestoreFhirIdFromExistingRecord() {
        QuestionnaireResponseRecord existing = makeRecord("uuid-1", "p1", "in-progress");
        existing.setFhirId("original-fhir-id");
        when(dao.getQuestionnaireResponseByUuid("uuid-1")).thenReturn(existing);

        QuestionnaireResponse update = new QuestionnaireResponse();
        service.updateQuestionnaireResponse("uuid-1", update);

        assertThat(update.getIdElement().getIdPart(), is("original-fhir-id"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateQuestionnaireResponse_shouldThrowWhenNotFound() {
        when(dao.getQuestionnaireResponseByUuid("missing")).thenReturn(null);

        service.updateQuestionnaireResponse("missing", new QuestionnaireResponse());
    }

    @Test
    public void updateQuestionnaireResponse_shouldThrowWithUuidInMessage() {
        when(dao.getQuestionnaireResponseByUuid("specific-uuid")).thenReturn(null);

        try {
            service.updateQuestionnaireResponse("specific-uuid", new QuestionnaireResponse());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("specific-uuid"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // deleteQuestionnaireResponse
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void deleteQuestionnaireResponse_shouldDeleteExistingRecord() {
        QuestionnaireResponseRecord record = makeRecord("del-uuid", "p1", "completed");
        when(dao.getQuestionnaireResponseByUuid("del-uuid")).thenReturn(record);

        service.deleteQuestionnaireResponse("del-uuid");

        verify(dao).deleteQuestionnaireResponse(record);
    }

    @Test
    public void deleteQuestionnaireResponse_shouldDoNothingWhenNotFound() {
        when(dao.getQuestionnaireResponseByUuid("missing")).thenReturn(null);

        service.deleteQuestionnaireResponse("missing");

        verify(dao, never()).deleteQuestionnaireResponse(any());
    }
}
