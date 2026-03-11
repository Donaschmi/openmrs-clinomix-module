package org.openmrs.module.clinomix.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomix.api.ClinomixService;
import org.openmrs.module.clinomix.dao.ClinomixDao;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ClinomixService}.
 */
@Transactional
public class ClinomixServiceImpl extends BaseOpenmrsService implements ClinomixService {

    private ClinomixDao dao;

    public void setDao(ClinomixDao dao) {
        this.dao = dao;
    }

    public ClinomixDao getDao() {
        return dao;
    }

    @Override
    public String getModuleInfo() {
        return "Clinomix Module v1.0.0";
    }
}
