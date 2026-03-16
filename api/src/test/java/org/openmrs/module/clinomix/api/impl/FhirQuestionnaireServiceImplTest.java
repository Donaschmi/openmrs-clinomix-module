package org.openmrs.module.clinomix.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.clinomix.api.FhirClientFactory;
import org.openmrs.module.clinomix.api.FhirRestClient;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FhirQuestionnaireServiceImpl}.
 * FhirRestClient and AdministrationService are fully mocked.
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirQuestionnaireServiceImplTest {

    @Mock private FhirClientFactory fhirClientFactory;
    @Mock private FhirContext fhirContext;
    @Mock private AdministrationService administrationService;
    @Mock private FhirRestClient fhirRestClient;

    private FhirQuestionnaireServiceImpl service;

    @Before
    public void setUp() {
        service = new FhirQuestionnaireServiceImpl();
        service.setFhirClientFactory(fhirClientFactory);
        service.setFhirContext(fhirContext);
        service.setAdministrationService(administrationService);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static Bundle bundleOf(Questionnaire... questionnaires) {
        Bundle bundle = new Bundle();
        for (Questionnaire q : questionnaires) {
            bundle.addEntry().setResource(q);
        }
        return bundle;
    }

    private static Questionnaire questionnaire(String id, String title) {
        Questionnaire q = new Questionnaire();
        q.setId(id);
        q.setTitle(title);
        q.setStatus(Enumerations.PublicationStatus.ACTIVE);
        return q;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getQuestionnaires
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getQuestionnaires_shouldReturnAllQuestionnairesFromBundle() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.search(eq("Questionnaire"), isNull()))
                .thenReturn(bundleOf(
                        questionnaire("q-001", "Patient Intake Form"),
                        questionnaire("q-002", "PHQ-9 Depression Screening"),
                        questionnaire("q-003", "Antenatal Visit Assessment")));

        List<Questionnaire> result = service.getQuestionnaires();

        assertThat(result, hasSize(3));
        assertThat(result.get(0).getId(), containsString("q-001"));
        assertThat(result.get(1).getTitle(), is("PHQ-9 Depression Screening"));
    }

    @Test
    public void getQuestionnaires_shouldReturnEmptyListWhenBundleIsEmpty() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.search(eq("Questionnaire"), isNull())).thenReturn(new Bundle());

        List<Questionnaire> result = service.getQuestionnaires();

        assertThat(result, notNullValue());
        assertThat(result, hasSize(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getQuestionnaireById
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getQuestionnaireById_shouldReturnMatchingQuestionnaire() {
        Questionnaire expected = questionnaire("q-001", "Patient Intake Form");
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.read(Questionnaire.class, "q-001")).thenReturn(expected);

        Questionnaire result = service.getQuestionnaireById("q-001");

        assertThat(result, is(expected));
        assertThat(result.getTitle(), is("Patient Intake Form"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // searchQuestionnaires
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void searchQuestionnaires_shouldReturnAllResultsWhenNoFilters() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.search(eq("Questionnaire"), isNull()))
                .thenReturn(bundleOf(
                        questionnaire("q-001", "Patient Intake Form"),
                        questionnaire("q-002", "PHQ-9")));

        IBundleProvider result = service.searchQuestionnaires(null, null, null);

        assertThat(result, notNullValue());
        assertThat(result.size(), is(2));
    }

    @Test
    public void searchQuestionnaires_shouldApplyTitleFilter() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.search(eq("Questionnaire"), anyString()))
                .thenReturn(bundleOf(questionnaire("q-002", "PHQ-9 Depression Screening")));

        IBundleProvider result = service.searchQuestionnaires("PHQ", null, null);

        assertThat(result.size(), is(1));
    }

    @Test
    public void searchQuestionnaires_shouldApplyCountLimit() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.search(eq("Questionnaire"), anyString()))
                .thenReturn(bundleOf(questionnaire("q-001", "Patient Intake Form")));

        IBundleProvider result = service.searchQuestionnaires(null, 1, null);

        assertThat(result.size(), is(1));
        verify(fhirRestClient).search(eq("Questionnaire"), anyString());
    }

    @Test
    public void searchQuestionnaires_shouldReturnEmptyProviderWhenNoneFound() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.search(eq("Questionnaire"), anyString())).thenReturn(new Bundle());

        IBundleProvider result = service.searchQuestionnaires("nonexistent", null, null);

        assertThat(result.size(), is(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void createQuestionnaire_shouldReturnCreatedResource() {
        Questionnaire input = questionnaire(null, "New Form");
        Questionnaire created = questionnaire("q-new", "New Form");
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.create(input)).thenReturn(created);

        Questionnaire result = service.createQuestionnaire(input);

        assertThat(result.getId(), containsString("q-new"));
        assertThat(result.getTitle(), is("New Form"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void updateQuestionnaire_shouldSetIdAndReturnUpdatedResource() {
        Questionnaire input = questionnaire(null, "Updated Title");
        Questionnaire updated = questionnaire("q-001", "Updated Title");
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.update(any(Questionnaire.class), eq("q-001"))).thenReturn(updated);

        Questionnaire result = service.updateQuestionnaire("q-001", input);

        assertThat(input.getId(), containsString("q-001"));
        assertThat(result.getTitle(), is("Updated Title"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // deleteQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void deleteQuestionnaire_shouldInvokeDeleteOnFhirServer() {
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);

        service.deleteQuestionnaire("q-001");

        verify(fhirRestClient).delete("Questionnaire", "q-001");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Version history
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getVersionHistory_shouldReturnEmptyListWhenNoHistoryStored() {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(null);

        List<String> history = service.getVersionHistory("q-001");

        assertThat(history, notNullValue());
        assertThat(history, hasSize(0));
    }

    @Test
    public void archiveVersion_shouldAppendSnapshotToHistory() {
        when(administrationService.getGlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001"))
                .thenReturn(null);
        when(administrationService.getGlobalPropertyObject(anyString())).thenReturn(null);
        when(administrationService.saveGlobalProperty(any())).thenReturn(new GlobalProperty());

        service.archiveVersion("q-001", "{\"resourceType\":\"Questionnaire\",\"id\":\"q-001\"}");

        verify(administrationService).saveGlobalProperty(any(GlobalProperty.class));
    }

    @Test
    public void archiveVersion_shouldAppendToExistingHistory() {
        String existingJson = "[\"{\\\"resourceType\\\":\\\"Questionnaire\\\",\\\"id\\\":\\\"q-001\\\"}\"]";
        when(administrationService.getGlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001"))
                .thenReturn(existingJson);
        GlobalProperty gp = new GlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001", existingJson);
        when(administrationService.getGlobalPropertyObject(anyString())).thenReturn(gp);
        when(administrationService.saveGlobalProperty(any())).thenReturn(gp);

        service.archiveVersion("q-001", "{\"resourceType\":\"Questionnaire\",\"id\":\"q-001\",\"version\":\"2.0\"}");

        verify(administrationService).saveGlobalProperty(any(GlobalProperty.class));
        assertThat(gp.getPropertyValue(), containsString("2.0"));
    }

    @Test
    public void deleteArchivedVersion_shouldRemoveSnapshotAtIndex() {
        String stored = "[\"{\\\"id\\\":\\\"v1\\\"}\",\"{\\\"id\\\":\\\"v2\\\"}\"]";
        when(administrationService.getGlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001"))
                .thenReturn(stored);
        GlobalProperty gp = new GlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001", stored);
        when(administrationService.getGlobalPropertyObject(anyString())).thenReturn(gp);
        when(administrationService.saveGlobalProperty(any())).thenReturn(gp);

        service.deleteArchivedVersion("q-001", 0);

        assertThat(gp.getPropertyValue(), containsString("v2"));
        assertThat(gp.getPropertyValue(), org.hamcrest.Matchers.not(containsString("v1")));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void deleteArchivedVersion_shouldThrowWhenIndexOutOfRange() {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(null);

        service.deleteArchivedVersion("q-001", 5);
    }

    @Test
    public void restoreVersion_shouldUpdateQuestionnaireWithSnapshot() {
        String snapshotJson = "{\"resourceType\":\"Questionnaire\",\"id\":\"q-001\",\"title\":\"Old Title\"}";
        String stored = "[\"" + snapshotJson.replace("\"", "\\\"") + "\"]";
        Questionnaire restored = questionnaire("q-001", "Old Title");

        when(administrationService.getGlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001"))
                .thenReturn(stored);
        when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4Cached().newJsonParser());
        when(fhirClientFactory.getClient()).thenReturn(fhirRestClient);
        when(fhirRestClient.update(any(Questionnaire.class), eq("q-001"))).thenReturn(restored);

        Questionnaire result = service.restoreVersion("q-001", 0);

        assertThat(result.getTitle(), is("Old Title"));
        verify(fhirRestClient).update(any(Questionnaire.class), eq("q-001"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void restoreVersion_shouldThrowWhenIndexOutOfRange() {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(null);

        service.restoreVersion("q-001", 0);
    }
}
