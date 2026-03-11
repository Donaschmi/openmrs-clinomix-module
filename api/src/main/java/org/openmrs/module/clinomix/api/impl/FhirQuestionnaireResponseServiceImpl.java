package org.openmrs.module.clinomix.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomix.ClinomixConstants;
import org.openmrs.module.clinomix.api.FhirQuestionnaireResponseService;
import org.openmrs.module.clinomix.dao.ClinomixDao;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Default implementation of {@link FhirQuestionnaireResponseService}.
 *
 * TODO: Replace stub methods with real persistence via Hibernate entities or
 *       delegation to HAPI FHIR JPA.
 */
@Transactional
public class FhirQuestionnaireResponseServiceImpl extends BaseOpenmrsService implements FhirQuestionnaireResponseService {

    private FhirContext fhirContext;

    private ClinomixDao dao;

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public void setDao(ClinomixDao dao) {
        this.dao = dao;
    }

    /**
     * Returns a HAPI FHIR generic client pointing at the configured FHIR server URL.
     * The URL is read from the OpenMRS global property {@code clinomix.fhirServerUrl}
     * on every call so that admin changes take effect without a server restart.
     */
    protected IGenericClient getFhirClient() {
        String url = Context.getAdministrationService()
                .getGlobalProperty(ClinomixConstants.GP_FHIR_SERVER_URL);
        if (StringUtils.isBlank(url)) {
            url = ClinomixConstants.GP_FHIR_SERVER_URL_DEFAULT;
        }
        return fhirContext.newRestfulGenericClient(url);
    }

    @Override
    public QuestionnaireResponse getQuestionnaireResponseById(String id) {
        // TODO: retrieve from persistence layer
        throw new UnsupportedOperationException("getQuestionnaireResponseById not yet implemented");
    }

    @Override
    public IBundleProvider searchQuestionnaireResponses(String questionnaireRef, String subjectRef, Integer count) {
        // TODO: query persistence layer filtering by questionnaire and/or subject
        return new SimpleBundleProvider(Collections.emptyList());
    }

    @Override
    public QuestionnaireResponse createQuestionnaireResponse(QuestionnaireResponse response) {
        // TODO: persist and return with server-assigned id
        throw new UnsupportedOperationException("createQuestionnaireResponse not yet implemented");
    }

    @Override
    public QuestionnaireResponse updateQuestionnaireResponse(String id, QuestionnaireResponse response) {
        // TODO: update the existing resource
        throw new UnsupportedOperationException("updateQuestionnaireResponse not yet implemented");
    }

    @Override
    public void deleteQuestionnaireResponse(String id) {
        // TODO: mark as deleted
        throw new UnsupportedOperationException("deleteQuestionnaireResponse not yet implemented");
    }
}
