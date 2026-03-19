package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireRecord;
import org.openmrs.module.clinomx.model.QuestionnaireResponseRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * FHIR R4 compliance tests for {@link FhirQuestionnaireServiceImpl} and
 * {@link FhirQuestionnaireResponseServiceImpl}.
 *
 * <p>These tests verify that every resource produced by the service layer conforms to the FHIR R4
 * specification:
 * <ul>
 *   <li>Correct {@code resourceType} on every returned object</li>
 *   <li>Required fields are populated ({@code id}, {@code status})</li>
 *   <li>Status codes belong to the correct FHIR value set</li>
 *   <li>Canonical URLs are valid absolute URIs</li>
 *   <li>References follow the FHIR {@code ResourceType/id} or absolute-URL format</li>
 *   <li>Item {@code linkId} and {@code type} are present on every item (and nested item)</li>
 *   <li>All data survives a FHIR JSON round-trip without loss</li>
 * </ul>
 *
 * <p>ClinomXDao is mocked; FhirContext is used as a real library instance so that HAPI FHIR
 * parsing/serialisation is exercised on every test.
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirComplianceTest {

    private static final FhirContext FHIR_CTX = FhirContext.forR4Cached();

    /**
     * Valid FHIR R4 {@code PublicationStatus} codes for {@code Questionnaire.status}.
     * Source: http://hl7.org/fhir/R4/valueset-publication-status.html
     */
    private static final Set<String> VALID_QUESTIONNAIRE_STATUSES =
            new HashSet<>(Arrays.asList("draft", "active", "retired", "unknown"));

    /**
     * Valid FHIR R4 codes for {@code QuestionnaireResponse.status}.
     * Source: http://hl7.org/fhir/R4/valueset-questionnaire-answers-status.html
     */
    private static final Set<String> VALID_RESPONSE_STATUSES =
            new HashSet<>(Arrays.asList("in-progress", "completed", "amended", "entered-in-error", "stopped"));

    @Mock private ClinomXDao dao;

    private FhirQuestionnaireServiceImpl questionnaireService;
    private FhirQuestionnaireResponseServiceImpl responseService;

    @Before
    public void setUp() {
        questionnaireService = new FhirQuestionnaireServiceImpl();
        questionnaireService.setFhirContext(FHIR_CTX);
        questionnaireService.setDao(dao);

        responseService = new FhirQuestionnaireResponseServiceImpl();
        responseService.setFhirContext(FHIR_CTX);
        responseService.setDao(dao);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Builds a minimal FHIR-valid Questionnaire (id + required status). */
    private static Questionnaire minimalQuestionnaire(String id) {
        Questionnaire q = new Questionnaire();
        q.setId(id);
        q.setStatus(Enumerations.PublicationStatus.ACTIVE);
        return q;
    }

    /** Builds a minimal FHIR-valid QuestionnaireResponse (id + required status). */
    private static QuestionnaireResponse minimalResponse(String id, String statusCode) {
        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId(id);
        qr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.fromCode(statusCode));
        return qr;
    }

    /** Converts a {@link Questionnaire} to a {@link QuestionnaireRecord} as the DAO would return it. */
    private QuestionnaireRecord toRecord(String uuid, Questionnaire q) {
        QuestionnaireRecord r = new QuestionnaireRecord();
        r.setUuid(uuid);
        r.setFhirId(q.getIdElement().getIdPart());
        r.setUrl(q.getUrl());
        r.setName(q.getName());
        r.setTitle(q.getTitle());
        r.setStatus(q.getStatus() != null ? q.getStatus().toCode() : null);
        r.setVersion(q.getVersion());
        r.setFhirJson(FHIR_CTX.newJsonParser().encodeResourceToString(q));
        r.setDateCreated(new Date());
        return r;
    }

    /** Converts a {@link QuestionnaireResponse} to a {@link QuestionnaireResponseRecord} as the DAO would return it. */
    private QuestionnaireResponseRecord toRecord(String uuid, QuestionnaireResponse qr) {
        QuestionnaireResponseRecord r = new QuestionnaireResponseRecord();
        r.setUuid(uuid);
        r.setFhirId(qr.getIdElement().getIdPart());
        r.setStatus(qr.getStatus() != null ? qr.getStatus().toCode() : null);
        r.setAuthored(qr.getAuthored());
        r.setFhirJson(FHIR_CTX.newJsonParser().encodeResourceToString(qr));
        r.setDateCreated(new Date());
        return r;
    }

    /** Recursively asserts that every item (and sub-item) in the list has a non-blank linkId. */
    private void assertNestedItemsHaveLinkId(List<Questionnaire.QuestionnaireItemComponent> items) {
        for (Questionnaire.QuestionnaireItemComponent item : items) {
            assertThat("every item (including nested) must have linkId per FHIR R4",
                    item.getLinkId(), not(emptyOrNullString()));
            if (item.hasItem()) {
                assertNestedItemsHaveLinkId(item.getItem());
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Questionnaire — FHIR R4 compliance
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void questionnaire_getAllShouldReturnCorrectResourceType() {
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(
                toRecord("uuid-1", minimalQuestionnaire("fhir-1"))));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        assertThat(results, not(empty()));
        for (Questionnaire q : results) {
            assertThat("resourceType must be 'Questionnaire' (FHIR R4 §4.3)",
                    q.fhirType(), is("Questionnaire"));
        }
    }

    @Test
    public void questionnaire_getAllShouldHaveNonNullIdOnEveryResource() {
        when(dao.getAllQuestionnaires()).thenReturn(Arrays.asList(
                toRecord("uuid-1", minimalQuestionnaire("fhir-1")),
                toRecord("uuid-2", minimalQuestionnaire("fhir-2"))));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        for (Questionnaire q : results) {
            assertThat("every REST resource must carry a logical id (FHIR R4 §3.1)",
                    q.getIdElement().getIdPart(), not(emptyOrNullString()));
        }
    }

    @Test
    public void questionnaire_getAllShouldExposeDbUuidNotInternalFhirId() {
        // The OpenMRS UUID — not the FHIR internal id — is used as the stable REST id.
        Questionnaire q = minimalQuestionnaire("internal-fhir-id");
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(
                toRecord("openmrs-db-uuid", q)));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        assertThat(results.get(0).getIdElement().getIdPart(), is("openmrs-db-uuid"));
    }

    @Test
    public void questionnaire_getAllShouldHaveStatusOnEveryResource() {
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(
                toRecord("uuid-1", minimalQuestionnaire("fhir-1"))));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        for (Questionnaire q : results) {
            assertThat("Questionnaire.status is required (cardinality 1..1) in FHIR R4",
                    q.getStatus(), notNullValue());
        }
    }

    @Test
    public void questionnaire_getAllStatusCodesMustBelongToFhirValueSet() {
        // Store one record per valid publication-status code to verify all are accepted.
        List<QuestionnaireRecord> records = new ArrayList<>();
        for (Enumerations.PublicationStatus s : Enumerations.PublicationStatus.values()) {
            if (s == Enumerations.PublicationStatus.NULL) continue;
            Questionnaire q = minimalQuestionnaire("fhir-" + s.toCode());
            q.setStatus(s);
            records.add(toRecord("uuid-" + s.toCode(), q));
        }
        when(dao.getAllQuestionnaires()).thenReturn(records);

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        assertThat(results, not(empty()));
        for (Questionnaire q : results) {
            assertThat("status must be from the FHIR R4 PublicationStatus value set",
                    VALID_QUESTIONNAIRE_STATUSES, hasItem(q.getStatus().toCode()));
        }
    }

    @Test
    public void questionnaire_getByUuidShouldBeFullyFhirCompliant() {
        Questionnaire q = minimalQuestionnaire("fhir-1");
        q.setTitle("Patient Intake Form");
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(toRecord("uuid-1", q));

        Questionnaire result = questionnaireService.getQuestionnaireByUuid("uuid-1");

        assertThat(result.fhirType(), is("Questionnaire"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getStatus(), notNullValue());
        assertThat(VALID_QUESTIONNAIRE_STATUSES, hasItem(result.getStatus().toCode()));
    }

    @Test
    public void questionnaire_canonicalUrlIfPresentMustBeAbsoluteUri() {
        Questionnaire q = minimalQuestionnaire("fhir-1");
        q.setUrl("https://example.org/fhir/Questionnaire/PHQ9");
        when(dao.getQuestionnaireByUuid("uuid-1")).thenReturn(toRecord("uuid-1", q));

        Questionnaire result = questionnaireService.getQuestionnaireByUuid("uuid-1");

        String url = result.getUrl();
        assertThat("url must be non-blank when originally set", url, not(emptyOrNullString()));
        assertThat("Questionnaire.url must be an absolute URI (http, https or urn) per FHIR R4 §11.16",
                url.startsWith("http://") || url.startsWith("https://") || url.startsWith("urn:"),
                is(true));
    }

    @Test
    public void questionnaire_itemsMustHaveLinkId() {
        // FHIR R4: Questionnaire.item.linkId is 1..1 (required)
        Questionnaire q = minimalQuestionnaire("fhir-1");
        Questionnaire.QuestionnaireItemComponent item = q.addItem();
        item.setLinkId("smoking-status");
        item.setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(toRecord("uuid-1", q)));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        for (Questionnaire result : results) {
            for (Questionnaire.QuestionnaireItemComponent i : result.getItem()) {
                assertThat("Questionnaire.item.linkId is required (1..1) in FHIR R4",
                        i.getLinkId(), not(emptyOrNullString()));
            }
        }
    }

    @Test
    public void questionnaire_itemsMustHaveType() {
        // FHIR R4: Questionnaire.item.type is 1..1 (required)
        Questionnaire q = minimalQuestionnaire("fhir-1");
        Questionnaire.QuestionnaireItemComponent item = q.addItem();
        item.setLinkId("weight");
        item.setType(Questionnaire.QuestionnaireItemType.DECIMAL);
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(toRecord("uuid-1", q)));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        for (Questionnaire result : results) {
            for (Questionnaire.QuestionnaireItemComponent i : result.getItem()) {
                assertThat("Questionnaire.item.type is required (1..1) in FHIR R4",
                        i.getType(), notNullValue());
                assertThat("item.type must not be the uninitialised NULL sentinel",
                        i.getType(), not(Questionnaire.QuestionnaireItemType.NULL));
            }
        }
    }

    @Test
    public void questionnaire_nestedGroupItemsMustAlsoHaveLinkId() {
        // FHIR R4 §11.16.4.2: the linkId constraint applies recursively to all items.
        Questionnaire q = minimalQuestionnaire("fhir-1");
        Questionnaire.QuestionnaireItemComponent group = q.addItem();
        group.setLinkId("section-1");
        group.setType(Questionnaire.QuestionnaireItemType.GROUP);
        Questionnaire.QuestionnaireItemComponent child = group.addItem();
        child.setLinkId("section-1.q1");
        child.setType(Questionnaire.QuestionnaireItemType.STRING);
        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(toRecord("uuid-1", q)));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();

        for (Questionnaire result : results) {
            assertNestedItemsHaveLinkId(result.getItem());
        }
    }

    @Test
    public void questionnaire_fhirJsonRoundTripMustPreserveAllFields() {
        // Encode → store → retrieve should not lose any data.
        Questionnaire original = minimalQuestionnaire("fhir-rt");
        original.setTitle("Diabetes Screening");
        original.setUrl("https://example.org/fhir/Questionnaire/diabetes");
        original.setName("DiabetesScreening");
        original.setVersion("2.1.0");
        Questionnaire.QuestionnaireItemComponent item = original.addItem();
        item.setLinkId("hba1c");
        item.setText("Latest HbA1c reading");
        item.setType(Questionnaire.QuestionnaireItemType.DECIMAL);

        when(dao.getAllQuestionnaires()).thenReturn(Collections.singletonList(
                toRecord("uuid-rt", original)));

        List<Questionnaire> results = questionnaireService.getAllQuestionnaires();
        Questionnaire result = results.get(0);

        assertThat(result.getTitle(), is("Diabetes Screening"));
        assertThat(result.getUrl(), is("https://example.org/fhir/Questionnaire/diabetes"));
        assertThat(result.getName(), is("DiabetesScreening"));
        assertThat(result.getVersion(), is("2.1.0"));
        assertThat(result.getItem(), hasSize(1));
        assertThat(result.getItem().get(0).getLinkId(), is("hba1c"));
        assertThat(result.getItem().get(0).getText(), is("Latest HbA1c reading"));
        assertThat(result.getItem().get(0).getType(), is(Questionnaire.QuestionnaireItemType.DECIMAL));
    }

    @Test
    public void questionnaire_searchByTitleShouldReturnFhirCompliantResources() {
        Questionnaire q = minimalQuestionnaire("fhir-1");
        q.setTitle("PHQ-9 Depression Screening");
        when(dao.searchQuestionnairesByTitle("PHQ")).thenReturn(Collections.singletonList(
                toRecord("uuid-1", q)));

        List<Questionnaire> results = questionnaireService.searchQuestionnairesByTitle("PHQ");

        assertThat(results, hasSize(1));
        Questionnaire result = results.get(0);
        assertThat(result.fhirType(), is("Questionnaire"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getStatus(), notNullValue());
    }

    @Test
    public void questionnaire_searchByNameShouldReturnFhirCompliantResources() {
        Questionnaire q = minimalQuestionnaire("fhir-1");
        q.setName("PHQ9");
        q.setUrl("https://example.org/fhir/Questionnaire/PHQ9");
        when(dao.searchQuestionnairesByName("PHQ9")).thenReturn(Collections.singletonList(
                toRecord("uuid-1", q)));

        List<Questionnaire> results = questionnaireService.searchQuestionnairesByName("PHQ9");

        assertThat(results, hasSize(1));
        Questionnaire result = results.get(0);
        assertThat(result.fhirType(), is("Questionnaire"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getName(), is("PHQ9"));
    }

    @Test
    public void questionnaire_getVersionsByUrlShouldReturnFhirCompliantResources() {
        String url = "https://example.org/fhir/Questionnaire/PHQ9";
        Questionnaire q1 = minimalQuestionnaire("fhir-1");
        q1.setUrl(url);
        q1.setVersion("1.0.0");
        Questionnaire q2 = minimalQuestionnaire("fhir-2");
        q2.setUrl(url);
        q2.setVersion("2.0.0");
        when(dao.getQuestionnairesByUrl(url)).thenReturn(Arrays.asList(
                toRecord("uuid-1", q1),
                toRecord("uuid-2", q2)));

        List<Questionnaire> results = questionnaireService.getVersionsByUrl(url);

        assertThat(results, hasSize(2));
        for (Questionnaire result : results) {
            assertThat(result.fhirType(), is("Questionnaire"));
            assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
            assertThat(result.getStatus(), notNullValue());
            // All versions share the same canonical URL.
            assertThat(result.getUrl(), is(url));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // QuestionnaireResponse — FHIR R4 compliance
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void questionnaireResponse_getAllShouldReturnCorrectResourceType() {
        when(dao.getAllQuestionnaireResponses()).thenReturn(Collections.singletonList(
                toRecord("uuid-1", minimalResponse("fhir-1", "completed"))));

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();

        assertThat(results, not(empty()));
        for (QuestionnaireResponse r : results) {
            assertThat("resourceType must be 'QuestionnaireResponse' (FHIR R4 §17.8)",
                    r.fhirType(), is("QuestionnaireResponse"));
        }
    }

    @Test
    public void questionnaireResponse_getAllShouldHaveNonNullIdOnEveryResource() {
        when(dao.getAllQuestionnaireResponses()).thenReturn(Arrays.asList(
                toRecord("uuid-1", minimalResponse("fhir-1", "completed")),
                toRecord("uuid-2", minimalResponse("fhir-2", "in-progress"))));

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();

        for (QuestionnaireResponse r : results) {
            assertThat("every REST resource must carry a logical id (FHIR R4 §3.1)",
                    r.getIdElement().getIdPart(), not(emptyOrNullString()));
        }
    }

    @Test
    public void questionnaireResponse_getAllShouldExposeDbUuidNotInternalFhirId() {
        QuestionnaireResponse qr = minimalResponse("internal-fhir-id", "completed");
        when(dao.getAllQuestionnaireResponses()).thenReturn(Collections.singletonList(
                toRecord("openmrs-db-uuid", qr)));

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();

        assertThat(results.get(0).getIdElement().getIdPart(), is("openmrs-db-uuid"));
    }

    @Test
    public void questionnaireResponse_getAllShouldHaveStatusOnEveryResource() {
        when(dao.getAllQuestionnaireResponses()).thenReturn(Collections.singletonList(
                toRecord("uuid-1", minimalResponse("fhir-1", "completed"))));

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();

        for (QuestionnaireResponse r : results) {
            assertThat("QuestionnaireResponse.status is required (1..1) in FHIR R4",
                    r.getStatus(), notNullValue());
        }
    }

    @Test
    public void questionnaireResponse_getAllStatusCodesMustBelongToFhirValueSet() {
        // Store one record per valid response-status code to verify all are accepted.
        List<QuestionnaireResponseRecord> records = new ArrayList<>();
        for (String code : VALID_RESPONSE_STATUSES) {
            records.add(toRecord("uuid-" + code, minimalResponse("fhir-" + code, code)));
        }
        when(dao.getAllQuestionnaireResponses()).thenReturn(records);

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();

        assertThat(results, hasSize(VALID_RESPONSE_STATUSES.size()));
        for (QuestionnaireResponse r : results) {
            assertThat("status must be from the FHIR R4 QuestionnaireResponseStatus value set",
                    VALID_RESPONSE_STATUSES, hasItem(r.getStatus().toCode()));
        }
    }

    @Test
    public void questionnaireResponse_getByUuidShouldBeFullyFhirCompliant() {
        when(dao.getQuestionnaireResponseByUuid("uuid-1")).thenReturn(
                toRecord("uuid-1", minimalResponse("fhir-1", "completed")));

        QuestionnaireResponse result = responseService.getQuestionnaireResponseByUuid("uuid-1");

        assertThat(result.fhirType(), is("QuestionnaireResponse"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getStatus(), notNullValue());
        assertThat(VALID_RESPONSE_STATUSES, hasItem(result.getStatus().toCode()));
    }

    @Test
    public void questionnaireResponse_questionnaireReferenceIfPresentMustFollowFhirFormat() {
        // FHIR R4: QuestionnaireResponse.questionnaire is a canonical reference.
        // Accepted formats: "Questionnaire/{id}" or absolute URL.
        QuestionnaireResponse qr = minimalResponse("fhir-1", "completed");
        qr.setQuestionnaire("Questionnaire/q-uuid");
        when(dao.getQuestionnaireResponseByUuid("uuid-1")).thenReturn(toRecord("uuid-1", qr));

        QuestionnaireResponse result = responseService.getQuestionnaireResponseByUuid("uuid-1");

        String ref = result.getQuestionnaire();
        assertThat("questionnaire reference must be preserved when originally set",
                ref, not(emptyOrNullString()));
        assertThat("questionnaire reference must be 'Questionnaire/{id}' or an absolute URL",
                ref.startsWith("Questionnaire/") || ref.startsWith("http://") || ref.startsWith("https://"),
                is(true));
    }

    @Test
    public void questionnaireResponse_subjectReferenceIfPresentMustFollowFhirFormat() {
        // FHIR R4: QuestionnaireResponse.subject is a Reference — the serialised form
        // must be a "ResourceType/id" literal reference or absolute URL.
        QuestionnaireResponse qr = minimalResponse("fhir-1", "completed");
        qr.setSubject(new Reference("Patient/patient-uuid-42"));
        when(dao.getQuestionnaireResponseByUuid("uuid-1")).thenReturn(toRecord("uuid-1", qr));

        QuestionnaireResponse result = responseService.getQuestionnaireResponseByUuid("uuid-1");

        assertThat("subject must be present when originally set", result.hasSubject(), is(true));
        String ref = result.getSubject().getReference();
        assertThat("subject.reference must be non-null", ref, notNullValue());
        // A FHIR relative reference contains a slash ("Patient/uuid");
        // an absolute URL also contains a slash.
        assertThat("subject.reference must follow 'ResourceType/id' or absolute-URL format",
                ref.contains("/"), is(true));
    }

    @Test
    public void questionnaireResponse_authoredDateIfPresentMustSurviveRoundTrip() {
        // FHIR dateTime serialisation uses second-level precision.
        // We allow up to 999 ms tolerance for truncation during JSON encoding.
        QuestionnaireResponse qr = minimalResponse("fhir-1", "completed");
        Date authored = new Date();
        qr.setAuthored(authored);
        when(dao.getQuestionnaireResponseByUuid("uuid-1")).thenReturn(toRecord("uuid-1", qr));

        QuestionnaireResponse result = responseService.getQuestionnaireResponseByUuid("uuid-1");

        assertThat("authored date must survive the FHIR JSON round-trip",
                result.getAuthored(), notNullValue());
        long diffMs = Math.abs(result.getAuthored().getTime() - authored.getTime());
        assertThat("authored date must match original within 1 second (FHIR dateTime precision)",
                diffMs, lessThan(1000L));
    }

    @Test
    public void questionnaireResponse_itemsMustHaveLinkId() {
        // FHIR R4: QuestionnaireResponse.item.linkId is 1..1 (required).
        QuestionnaireResponse qr = minimalResponse("fhir-1", "completed");
        QuestionnaireResponse.QuestionnaireResponseItemComponent item = qr.addItem();
        item.setLinkId("smoking-status");
        item.addAnswer().setValue(new BooleanType(false));
        when(dao.getAllQuestionnaireResponses()).thenReturn(Collections.singletonList(toRecord("uuid-1", qr)));

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();

        for (QuestionnaireResponse result : results) {
            for (QuestionnaireResponse.QuestionnaireResponseItemComponent i : result.getItem()) {
                assertThat("QuestionnaireResponse.item.linkId is required (1..1) in FHIR R4",
                        i.getLinkId(), not(emptyOrNullString()));
            }
        }
    }

    @Test
    public void questionnaireResponse_fhirJsonRoundTripMustPreserveAllFields() {
        // Encode → store → retrieve should not lose any data.
        QuestionnaireResponse original = minimalResponse("fhir-rt", "amended");
        original.setQuestionnaire("Questionnaire/q-uuid-42");
        original.setSubject(new Reference("Patient/patient-uuid-99"));
        original.setAuthored(new Date());
        QuestionnaireResponse.QuestionnaireResponseItemComponent item = original.addItem();
        item.setLinkId("phq-q1");
        item.addAnswer().setValue(new IntegerType(3));

        when(dao.getAllQuestionnaireResponses()).thenReturn(Collections.singletonList(
                toRecord("uuid-rt", original)));

        List<QuestionnaireResponse> results = responseService.getAllQuestionnaireResponses();
        QuestionnaireResponse result = results.get(0);

        assertThat(result.getStatus(),
                is(QuestionnaireResponse.QuestionnaireResponseStatus.AMENDED));
        assertThat(result.getQuestionnaire(), is("Questionnaire/q-uuid-42"));
        assertThat(result.getSubject().getReference(), is("Patient/patient-uuid-99"));
        assertThat(result.getAuthored(), notNullValue());
        assertThat(result.getItem(), hasSize(1));
        assertThat(result.getItem().get(0).getLinkId(), is("phq-q1"));
    }

    @Test
    public void questionnaireResponse_getResponsesByQuestionnaireShouldReturnFhirCompliantResources() {
        when(dao.getResponsesByQuestionnaire("q-uuid")).thenReturn(Collections.singletonList(
                toRecord("uuid-1", minimalResponse("fhir-1", "completed"))));

        List<QuestionnaireResponse> results = responseService.getResponsesByQuestionnaire("q-uuid");

        assertThat(results, hasSize(1));
        QuestionnaireResponse result = results.get(0);
        assertThat(result.fhirType(), is("QuestionnaireResponse"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getStatus(), notNullValue());
        assertThat(VALID_RESPONSE_STATUSES, hasItem(result.getStatus().toCode()));
    }

    @Test
    public void questionnaireResponse_getResponsesByPatientShouldReturnFhirCompliantResources() {
        when(dao.getResponsesByPatient("patient-uuid")).thenReturn(Collections.singletonList(
                toRecord("uuid-1", minimalResponse("fhir-1", "in-progress"))));

        List<QuestionnaireResponse> results = responseService.getResponsesByPatient("patient-uuid");

        assertThat(results, hasSize(1));
        QuestionnaireResponse result = results.get(0);
        assertThat(result.fhirType(), is("QuestionnaireResponse"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getStatus(), notNullValue());
    }

    @Test
    public void questionnaireResponse_getResponsesByQuestionnaireAndPatientShouldReturnFhirCompliantResources() {
        when(dao.getResponsesByQuestionnaireAndPatient("q-uuid", "p-uuid")).thenReturn(
                Collections.singletonList(toRecord("uuid-1", minimalResponse("fhir-1", "completed"))));

        List<QuestionnaireResponse> results =
                responseService.getResponsesByQuestionnaireAndPatient("q-uuid", "p-uuid");

        assertThat(results, hasSize(1));
        QuestionnaireResponse result = results.get(0);
        assertThat(result.fhirType(), is("QuestionnaireResponse"));
        assertThat(result.getIdElement().getIdPart(), not(emptyOrNullString()));
        assertThat(result.getStatus(), notNullValue());
    }
}
