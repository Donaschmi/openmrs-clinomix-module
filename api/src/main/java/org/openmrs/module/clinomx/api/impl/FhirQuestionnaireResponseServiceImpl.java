package org.openmrs.module.clinomx.api.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomx.api.FhirClientFactory;
import org.openmrs.module.clinomx.api.FhirQuestionnaireResponseService;
import org.openmrs.module.clinomx.api.FhirRestClient;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link FhirQuestionnaireResponseService}.
 * Delegates FHIR resource storage to an external HAPI FHIR JPA server via
 * {@link FhirClientFactory}, using Spring RestTemplate for HTTP transport.
 */
@Transactional
public class FhirQuestionnaireResponseServiceImpl extends BaseOpenmrsService implements FhirQuestionnaireResponseService {

    private FhirClientFactory fhirClientFactory;
    private ClinomXDao dao;

    public void setFhirClientFactory(FhirClientFactory fhirClientFactory) {
        this.fhirClientFactory = fhirClientFactory;
    }

    public void setDao(ClinomXDao dao) {
        this.dao = dao;
    }

    @Override
    public QuestionnaireResponse getQuestionnaireResponseById(String id) {
        return fhirClientFactory.getClient().read(QuestionnaireResponse.class, id);
    }

    @Override
    public IBundleProvider searchQuestionnaireResponses(String questionnaireRef, String subjectRef, Integer count) {
        StringBuilder params = new StringBuilder();
        if (StringUtils.isNotBlank(questionnaireRef)) {
            params.append("questionnaire=").append(questionnaireRef);
        }
        if (StringUtils.isNotBlank(subjectRef)) {
            if (params.length() > 0) params.append("&");
            params.append("subject=").append(subjectRef);
        }
        if (count != null) {
            if (params.length() > 0) params.append("&");
            params.append("_count=").append(count);
        }

        Bundle bundle = fhirClientFactory.getClient()
                .search("QuestionnaireResponse", params.length() > 0 ? params.toString() : null);
        List<IBaseResource> resources = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof QuestionnaireResponse)
                .map(e -> (IBaseResource) e.getResource())
                .collect(Collectors.toList());

        return new SimpleBundleProvider(resources);
    }

    @Override
    public QuestionnaireResponse createQuestionnaireResponse(QuestionnaireResponse response) {
        FhirRestClient client = fhirClientFactory.getClient();
        ensureAuthorExists(client, response);
        ensurePatientExists(client, response);
        return client.create(response);
    }

    @Override
    public QuestionnaireResponse updateQuestionnaireResponse(String id, QuestionnaireResponse response) {
        response.setId(id);
        return fhirClientFactory.getClient().update(response, id);
    }

    @Override
    public void deleteQuestionnaireResponse(String id) {
        fhirClientFactory.getClient().delete("QuestionnaireResponse", id);
    }

    // ── ensure referenced resources exist in HAPI ─────────────────────────────

    private void ensureAuthorExists(FhirRestClient client, QuestionnaireResponse response) {
        if (!response.hasAuthor()) return;
        String ref = response.getAuthor().getReference();
        if (StringUtils.isBlank(ref)) return;

        String id = ref.startsWith("Practitioner/") ? ref.substring("Practitioner/".length()) : ref;
        if (!client.exists("Practitioner", id)) {
            client.update(buildPractitionerFromOpenMRS(id), id);
        }
    }

    private void ensurePatientExists(FhirRestClient client, QuestionnaireResponse response) {
        if (!response.hasSubject()) return;
        String ref = response.getSubject().getReference();
        if (StringUtils.isBlank(ref)) return;

        String id = ref.startsWith("Patient/") ? ref.substring("Patient/".length()) : ref;
        if (!client.exists("Patient", id)) {
            client.update(buildPatientFromOpenMRS(id), id);
        }
    }

    private Practitioner buildPractitionerFromOpenMRS(String uuid) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(uuid);

        Provider provider = Context.getProviderService().getProviderByUuid(uuid);
        if (provider != null) {
            Person person = provider.getPerson();
            if (person != null) {
                PersonName personName = person.getPersonName();
                if (personName != null) {
                    HumanName name = new HumanName();
                    if (StringUtils.isNotBlank(personName.getFamilyName())) name.setFamily(personName.getFamilyName());
                    if (StringUtils.isNotBlank(personName.getGivenName()))  name.addGiven(personName.getGivenName());
                    if (StringUtils.isNotBlank(personName.getMiddleName())) name.addGiven(personName.getMiddleName());
                    practitioner.addName(name);
                }
            } else if (StringUtils.isNotBlank(provider.getName())) {
                practitioner.addName(new HumanName().setText(provider.getName()));
            }
        }
        return practitioner;
    }

    private Patient buildPatientFromOpenMRS(String uuid) {
        Patient fhirPatient = new Patient();
        fhirPatient.setId(uuid);

        org.openmrs.Patient omrsPatient = Context.getPatientService().getPatientByUuid(uuid);
        if (omrsPatient != null) {
            PersonName personName = omrsPatient.getPersonName();
            if (personName != null) {
                HumanName name = new HumanName();
                if (StringUtils.isNotBlank(personName.getFamilyName())) name.setFamily(personName.getFamilyName());
                if (StringUtils.isNotBlank(personName.getGivenName()))  name.addGiven(personName.getGivenName());
                if (StringUtils.isNotBlank(personName.getMiddleName())) name.addGiven(personName.getMiddleName());
                fhirPatient.addName(name);
            }
            if (omrsPatient.getBirthdate() != null) {
                fhirPatient.setBirthDate(omrsPatient.getBirthdate());
            }
            if (StringUtils.isNotBlank(omrsPatient.getGender())) {
                fhirPatient.setGender("F".equalsIgnoreCase(omrsPatient.getGender())
                        ? org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE
                        : org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);
            }
        }
        return fhirPatient;
    }
}
