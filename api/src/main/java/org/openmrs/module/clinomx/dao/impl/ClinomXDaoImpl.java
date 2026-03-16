package org.openmrs.module.clinomx.dao.impl;

import org.hibernate.SessionFactory;
import org.openmrs.module.clinomx.dao.ClinomXDao;

/**
 * Hibernate implementation of {@link ClinomXDao}.
 */
public class ClinomXDaoImpl implements ClinomXDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
