package org.openmrs.module.clinomix.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.clinomix.ClinomixPrivileges;
import org.springframework.transaction.annotation.Transactional;

/**
 * The main service interface for the Clinomix module.
 * <p>
 * This service handles business logic operations for the Clinomix application.
 * </p>
 */
@Transactional
public interface ClinomixService extends OpenmrsService {

    /**
     * Example method — replace with actual business methods needed by the Clinomix app.
     *
     * @return a greeting string
     */
    @Authorized(ClinomixPrivileges.GET_CLINOMIX_DATA)
    String getModuleInfo();
}
