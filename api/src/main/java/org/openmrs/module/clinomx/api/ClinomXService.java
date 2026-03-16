package org.openmrs.module.clinomx.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomx.ClinomXPrivileges;
import org.springframework.transaction.annotation.Transactional;

/**
 * The main service interface for the ClinomX module.
 * <p>
 * This service handles business logic operations for the ClinomX application.
 * </p>
 */
@Transactional
public interface ClinomXService extends OpenmrsService {

    /**
     * Example method — replace with actual business methods needed by the ClinomX app.
     *
     * @return a greeting string
     */
    @Authorized(ClinomXPrivileges.GET_CLINOM_X_DATA)
    String getModuleInfo();
}
