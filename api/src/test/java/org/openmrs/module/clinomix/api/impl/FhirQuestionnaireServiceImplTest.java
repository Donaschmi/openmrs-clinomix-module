package org.openmrs.module.clinomix.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDelete;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FhirQuestionnaireServiceImpl}.
 * All HAPI FHIR client chains and the AdministrationService are fully mocked.
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirQuestionnaireServiceImplTest {

    // ── mocks ──────────────────────────────────────────────────────────────────

    @Mock private FhirClientFactory fhirClientFactory;
    @Mock private FhirContext fhirContext;
    @Mock private AdministrationService administrationService;
    @Mock private IGenericClient fhirClient;

    // search chain
    @SuppressWarnings("rawtypes") @Mock private IUntypedQuery untypedQuery;
    @SuppressWarnings("rawtypes") @Mock private IQuery bundleQuery;

    // read chain
    @Mock private IRead readOp;
    @SuppressWarnings("rawtypes") @Mock private IReadTyped readTyped;
    @SuppressWarnings("rawtypes") @Mock private IReadExecutable readExecutable;

    // create chain
    @Mock private ICreate createOp;
    @Mock private ICreateTyped createTyped;

    // update chain
    @Mock private IUpdate updateOp;
    @Mock private IUpdateTyped updateTyped;

    // delete chain
    @Mock private IDelete deleteOp;
    @Mock private IDeleteTyped deleteTyped;

    // ── subject ────────────────────────────────────────────────────────────────

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

    /** Stubs the search().forResource().returnBundle().execute() chain. */
    @SuppressWarnings("unchecked")
    private void stubSearch(Bundle result) {
        when(fhirClientFactory.getClient()).thenReturn(fhirClient);
        when(fhirClient.search()).thenReturn((IUntypedQuery) untypedQuery);
        when(untypedQuery.forResource(Questionnaire.class)).thenReturn(bundleQuery);
        when(bundleQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
        when(bundleQuery.where(any(ICriterion.class))).thenReturn(bundleQuery);
        when(bundleQuery.count(anyInt())).thenReturn(bundleQuery);
        when(bundleQuery.execute()).thenReturn(result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getQuestionnaires
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void getQuestionnaires_shouldReturnAllQuestionnairesFromBundle() {
        stubSearch(bundleOf(
                questionnaire("q-001", "Patient Intake Form"),
                questionnaire("q-002", "PHQ-9 Depression Screening"),
                questionnaire("q-003", "Antenatal Visit Assessment")));

        List<Questionnaire> result = service.getQuestionnaires();

        assertThat(result, hasSize(3));
        assertThat(result.get(0).getId(), is("q-001"));
        assertThat(result.get(1).getTitle(), is("PHQ-9 Depression Screening"));
        assertThat(result.get(2).getTitle(), is("Antenatal Visit Assessment"));
    }

    @Test
    public void getQuestionnaires_shouldReturnEmptyListWhenBundleIsEmpty() {
        stubSearch(new Bundle());

        List<Questionnaire> result = service.getQuestionnaires();

        assertThat(result, notNullValue());
        assertThat(result, hasSize(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getQuestionnaireById
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @SuppressWarnings("unchecked")
    public void getQuestionnaireById_shouldReturnMatchingQuestionnaire() {
        Questionnaire expected = questionnaire("q-001", "Patient Intake Form");
        when(fhirClientFactory.getClient()).thenReturn(fhirClient);
        when(fhirClient.read()).thenReturn(readOp);
        when(readOp.resource(Questionnaire.class)).thenReturn(readTyped);
        when(readTyped.withId("q-001")).thenReturn(readExecutable);
        when(readExecutable.execute()).thenReturn(expected);

        Questionnaire result = service.getQuestionnaireById("q-001");

        assertThat(result, is(expected));
        assertThat(result.getTitle(), is("Patient Intake Form"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // searchQuestionnaires
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void searchQuestionnaires_shouldReturnAllResultsWhenNoFilters() {
        stubSearch(bundleOf(
                questionnaire("q-001", "Patient Intake Form"),
                questionnaire("q-002", "PHQ-9")));

        IBundleProvider result = service.searchQuestionnaires(null, null, null);

        assertThat(result, notNullValue());
        assertThat(result.size(), is(2));
    }

    @Test
    public void searchQuestionnaires_shouldApplyTitleFilter() {
        stubSearch(bundleOf(questionnaire("q-002", "PHQ-9 Depression Screening")));

        IBundleProvider result = service.searchQuestionnaires("PHQ", null, null);

        assertThat(result.size(), is(1));
    }

    @Test
    public void searchQuestionnaires_shouldApplyCountLimit() {
        stubSearch(bundleOf(questionnaire("q-001", "Patient Intake Form")));

        IBundleProvider result = service.searchQuestionnaires(null, 1, null);

        assertThat(result.size(), is(1));
        verify(bundleQuery).count(1);
    }

    @Test
    public void searchQuestionnaires_shouldReturnEmptyProviderWhenNoneFound() {
        stubSearch(new Bundle());

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
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(created);

        when(fhirClientFactory.getClient()).thenReturn(fhirClient);
        when(fhirClient.create()).thenReturn(createOp);
        when(createOp.resource(input)).thenReturn(createTyped);
        when(createTyped.execute()).thenReturn(outcome);

        Questionnaire result = service.createQuestionnaire(input);

        assertThat(result.getId(), is("q-new"));
        assertThat(result.getTitle(), is("New Form"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void updateQuestionnaire_shouldSetIdAndReturnUpdatedResource() {
        Questionnaire input = questionnaire(null, "Updated Title");
        Questionnaire updated = questionnaire("q-001", "Updated Title");
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(updated);

        when(fhirClientFactory.getClient()).thenReturn(fhirClient);
        when(fhirClient.update()).thenReturn(updateOp);
        when(updateOp.resource(input)).thenReturn(updateTyped);
        when(updateTyped.execute()).thenReturn(outcome);

        Questionnaire result = service.updateQuestionnaire("q-001", input);

        assertThat(input.getId(), is("q-001")); // id was set on input
        assertThat(result.getTitle(), is("Updated Title"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // deleteQuestionnaire
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void deleteQuestionnaire_shouldInvokeDeleteOnFhirServer() {
        when(fhirClientFactory.getClient()).thenReturn(fhirClient);
        when(fhirClient.delete()).thenReturn(deleteOp);
        when(deleteOp.resourceById("Questionnaire", "q-001")).thenReturn(deleteTyped);

        service.deleteQuestionnaire("q-001");

        verify(deleteTyped).execute();
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

        // The saved GP value should contain both snapshots
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

        // After deletion of index 0, only v2 should remain
        assertThat(gp.getPropertyValue(), containsString("v2"));
        assertThat(gp.getPropertyValue(), org.hamcrest.Matchers.not(containsString("v1")));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void deleteArchivedVersion_shouldThrowWhenIndexOutOfRange() {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(null);

        service.deleteArchivedVersion("q-001", 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void restoreVersion_shouldUpdateQuestionnaireWithSnapshot() {
        String snapshotJson = "{\"resourceType\":\"Questionnaire\",\"id\":\"q-001\",\"title\":\"Old Title\"}";
        String stored = "[\"" + snapshotJson.replace("\"", "\\\"") + "\"]";
        Questionnaire parsedSnapshot = questionnaire("q-001", "Old Title");
        Questionnaire restored = questionnaire("q-001", "Old Title");
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(restored);

        when(administrationService.getGlobalProperty(
                FhirQuestionnaireServiceImpl.HISTORY_GP_PREFIX + "q-001"))
                .thenReturn(stored);
        when(fhirContext.newJsonParser()).thenReturn(
                FhirContext.forR4Cached().newJsonParser());
        when(fhirClientFactory.getClient()).thenReturn(fhirClient);
        when(fhirClient.update()).thenReturn(updateOp);
        when(updateOp.resource(any(Questionnaire.class))).thenReturn(updateTyped);
        when(updateTyped.execute()).thenReturn(outcome);

        Questionnaire result = service.restoreVersion("q-001", 0);

        assertThat(result.getTitle(), is("Old Title"));
        verify(updateTyped).execute();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void restoreVersion_shouldThrowWhenIndexOutOfRange() {
        when(administrationService.getGlobalProperty(anyString())).thenReturn(null);

        service.restoreVersion("q-001", 0);
    }
}
