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

import org.openmrs.api.APIException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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

    private static Questionnaire questionnaire(String id, String name, String title, String version) {
        Questionnaire q = questionnaire(id, title);
        q.setUrl("http://example.org/fhir/Questionnaire/" + (name != null ? name : id));
        q.setName(name);
        q.setVersion(version);
        return q;
    }

    private QuestionnaireRecord recordFor(String uuid, Questionnaire q) {
        QuestionnaireRecord r = new QuestionnaireRecord();
        r.setUuid(uuid);
        r.setFhirId(q.getIdElement().getIdPart());
        r.setUrl(q.getUrl());
        r.setName(q.getName());
        r.setTitle(q.getTitle());
        r.setStatus(q.getStatus().toCode());
        r.setVersion(q.getVersion());
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
        Questionnaire input = questionnaire("fhir-new", "PHQ9", "New Form", "1.0.0");

        service.createQuestionnaire(input);

        ArgumentCaptor<QuestionnaireRecord> captor = ArgumentCaptor.forClass(QuestionnaireRecord.class);
        verify(dao).saveQuestionnaire(captor.capture());

        QuestionnaireRecord saved = captor.getValue();
        assertThat(saved.getUrl(), is("http://example.org/fhir/Questionnaire/PHQ9"));
        assertThat(saved.getName(), is("PHQ9"));
        assertThat(saved.getTitle(), is("New Form"));
        assertThat(saved.getStatus(), is("active"));
        assertThat(saved.getVersion(), is("1.0.0"));
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

    @Test(expected = APIException.class)
    public void createQuestionnaire_shouldThrowWhenUrlAndVersionAlreadyExist() {
        QuestionnaireRecord duplicate = recordFor("uuid-existing",
                questionnaire("fhir-1", "PHQ9", "PHQ-9", "1.0.0"));
        when(dao.getQuestionnaireByUrlAndVersion(
                "http://example.org/fhir/Questionnaire/PHQ9", "1.0.0"))
                .thenReturn(duplicate);

        service.createQuestionnaire(questionnaire("fhir-new", "PHQ9", "PHQ-9", "1.0.0"));
    }

    @Test
    public void createQuestionnaire_shouldSucceedWhenUrlExistsButVersionDiffers() {
        // url is the same but version is different → new version of an existing questionnaire
        when(dao.getQuestionnaireByUrlAndVersion(
                "http://example.org/fhir/Questionnaire/PHQ9", "2.0.0"))
                .thenReturn(null);

        Questionnaire input = questionnaire("fhir-new", "PHQ9", "PHQ-9", "2.0.0");
        service.createQuestionnaire(input); // must not throw

        verify(dao).saveQuestionnaire(any(QuestionnaireRecord.class));
    }

    @Test
    public void createQuestionnaire_shouldSucceedWhenUrlIsAbsent() {
        // Questionnaires without a canonical url have no conflict identity — always allowed
        Questionnaire input = questionnaire("fhir-local", "Local Form");
        // input has no url set — no DAO call should be made for conflict check
        service.createQuestionnaire(input);

        verify(dao).saveQuestionnaire(any(QuestionnaireRecord.class));
        verify(dao, never()).getQuestionnaireByUrlAndVersion(any(), any());
    }

    @Test(expected = APIException.class)
    public void updateQuestionnaire_shouldThrowWhenNewVersionCollidesWithDifferentRecord() {
        // Record being updated
        QuestionnaireRecord target = recordFor("uuid-target",
                questionnaire("fhir-1", "PHQ9", "PHQ-9", "1.0.0"));
        when(dao.getQuestionnaireByUuid("uuid-target")).thenReturn(target);

        // A different record already owns url + version "2.0.0"
        QuestionnaireRecord other = recordFor("uuid-other",
                questionnaire("fhir-2", "PHQ9", "PHQ-9", "2.0.0"));
        when(dao.getQuestionnaireByUrlAndVersion(
                "http://example.org/fhir/Questionnaire/PHQ9", "2.0.0"))
                .thenReturn(other);

        // Trying to update uuid-target to version 2.0.0 must fail
        service.updateQuestionnaire("uuid-target",
                questionnaire("fhir-1", "PHQ9", "PHQ-9 Updated", "2.0.0"));
    }

    @Test
    public void updateQuestionnaire_shouldSucceedWhenResavingWithSameUrlAndVersion() {
        // Updating a questionnaire in-place (same url+version) must not conflict with itself
        Questionnaire original = questionnaire("fhir-1", "PHQ9", "PHQ-9", "1.0.0");
        QuestionnaireRecord record = recordFor("uuid-1", original);
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(record);

        // The conflict query returns the same record (same uuid)
        when(dao.getQuestionnaireByUrlAndVersion(
                "http://example.org/fhir/Questionnaire/PHQ9", "1.0.0"))
                .thenReturn(record);

        Questionnaire update = questionnaire("fhir-1", "PHQ9", "PHQ-9 corrected title", "1.0.0");
        service.updateQuestionnaire("uuid-1", update); // must not throw

        verify(dao).saveQuestionnaire(any(QuestionnaireRecord.class));
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

    // ══════════════════════════════════════════════════════════════════════════
    // searchQuestionnairesByName
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void searchQuestionnairesByName_shouldReturnMatchingQuestionnaires() {
        QuestionnaireRecord r = recordFor("uuid-1", questionnaire("fhir-1", "PHQ9", "PHQ-9 Depression Screening", "1.0.0"));
        when(dao.searchQuestionnairesByName("PHQ9")).thenReturn(Collections.singletonList(r));

        List<Questionnaire> result = service.searchQuestionnairesByName("PHQ9");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getName(), is("PHQ9"));
    }

    @Test
    public void searchQuestionnairesByName_shouldReturnEmptyListWhenNoMatch() {
        when(dao.searchQuestionnairesByName("Unknown")).thenReturn(Collections.emptyList());

        List<Questionnaire> result = service.searchQuestionnairesByName("Unknown");

        assertThat(result, hasSize(0));
    }

    @Test
    public void createQuestionnaire_shouldPersistUrlNameAndVersion() {
        Questionnaire input = questionnaire("fhir-x", "PatientIntake", "Patient Intake Form", "2.0.0");

        service.createQuestionnaire(input);

        ArgumentCaptor<QuestionnaireRecord> captor = ArgumentCaptor.forClass(QuestionnaireRecord.class);
        verify(dao).saveQuestionnaire(captor.capture());
        assertThat(captor.getValue().getUrl(), is("http://example.org/fhir/Questionnaire/PatientIntake"));
        assertThat(captor.getValue().getName(), is("PatientIntake"));
        assertThat(captor.getValue().getVersion(), is("2.0.0"));
    }

    @Test
    public void updateQuestionnaire_shouldUpdateUrlNameAndVersion() {
        QuestionnaireRecord existing = recordFor("uuid-1",
                questionnaire("fhir-1", "PHQ9", "PHQ-9", "1.0.0"));
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(existing);

        Questionnaire update = questionnaire(null, "PHQ9", "PHQ-9 Updated", "1.1.0");
        service.updateQuestionnaire("uuid-1", update);

        ArgumentCaptor<QuestionnaireRecord> captor = ArgumentCaptor.forClass(QuestionnaireRecord.class);
        verify(dao).saveQuestionnaire(captor.capture());
        assertThat(captor.getValue().getUrl(), is("http://example.org/fhir/Questionnaire/PHQ9"));
        assertThat(captor.getValue().getName(), is("PHQ9"));
        assertThat(captor.getValue().getVersion(), is("1.1.0"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getVersionsByUrl — FHIR-compliant version comparison via canonical URL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getVersionsByUrl_shouldReturnVersionsInAscendingSemverOrder() {
        String canonicalUrl = "http://example.org/fhir/Questionnaire/PHQ9";
        QuestionnaireRecord v200 = recordFor("uuid-v200", questionnaire("fhir-3", "PHQ9", "PHQ-9", "2.0.0"));
        QuestionnaireRecord v101 = recordFor("uuid-v101", questionnaire("fhir-2", "PHQ9", "PHQ-9", "1.0.1"));
        QuestionnaireRecord v100 = recordFor("uuid-v100", questionnaire("fhir-1", "PHQ9", "PHQ-9", "1.0.0"));
        // DAO returns them out of insertion order — service must sort by semver
        when(dao.getQuestionnairesByUrl(canonicalUrl)).thenReturn(Arrays.asList(v200, v101, v100));

        List<Questionnaire> result = service.getVersionsByUrl(canonicalUrl);

        assertThat(result, hasSize(3));
        assertThat(result.get(0).getVersion(), is("1.0.0"));
        assertThat(result.get(1).getVersion(), is("1.0.1"));
        assertThat(result.get(2).getVersion(), is("2.0.0"));
    }

    @Test
    public void getVersionsByUrl_shouldReturnEmptyListWhenNoMatch() {
        String canonicalUrl = "http://example.org/fhir/Questionnaire/NonExistent";
        when(dao.getQuestionnairesByUrl(canonicalUrl)).thenReturn(Collections.emptyList());

        List<Questionnaire> result = service.getVersionsByUrl(canonicalUrl);

        assertThat(result, hasSize(0));
    }

    @Test
    public void getVersionsByUrl_shouldHandleNullVersionsGracefully() {
        String canonicalUrl = "http://example.org/fhir/Questionnaire/PHQ9";
        // A record with no version should sort before versioned ones (treated as 0.0.0)
        QuestionnaireRecord noVersion = recordFor("uuid-nv", questionnaire("fhir-nv", "PHQ9", "PHQ-9", null));
        QuestionnaireRecord v100 = recordFor("uuid-v100", questionnaire("fhir-1", "PHQ9", "PHQ-9", "1.0.0"));
        when(dao.getQuestionnairesByUrl(canonicalUrl)).thenReturn(Arrays.asList(v100, noVersion));

        List<Questionnaire> result = service.getVersionsByUrl(canonicalUrl);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getVersion(), nullValue());
        assertThat(result.get(1).getVersion(), is("1.0.0"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEMVER_ORDER — unit tests for the comparator
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void semverOrder_shouldOrderVersionsCorrectly() {
        assertThat(FhirQuestionnaireServiceImpl.SEMVER_ORDER.compare("1.0.0", "2.0.0") < 0, is(true));
        assertThat(FhirQuestionnaireServiceImpl.SEMVER_ORDER.compare("1.0.1", "1.0.0") > 0, is(true));
        assertThat(FhirQuestionnaireServiceImpl.SEMVER_ORDER.compare("1.2.0", "1.2.0") == 0, is(true));
        assertThat(FhirQuestionnaireServiceImpl.SEMVER_ORDER.compare("1.10.0", "1.9.0") > 0, is(true));
        assertThat(FhirQuestionnaireServiceImpl.SEMVER_ORDER.compare(null, "1.0.0") < 0, is(true));
        assertThat(FhirQuestionnaireServiceImpl.SEMVER_ORDER.compare("1.0.0", null) > 0, is(true));
    }
}
