package org.openmrs.module.clinomx.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.clinomx.api.ClinomXService;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ClinomXService}.
 */
@Transactional
public class ClinomXServiceImpl extends BaseOpenmrsService implements ClinomXService {

    private ClinomXDao dao;

    public void setDao(ClinomXDao dao) {
        this.dao = dao;
    }

    public ClinomXDao getDao() {
        return dao;
    }

    @Override
    public String getModuleInfo() {
        return "ClinomX Module v1.0.0";
    }
}
