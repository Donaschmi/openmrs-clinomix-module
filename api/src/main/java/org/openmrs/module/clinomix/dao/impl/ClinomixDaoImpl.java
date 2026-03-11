package org.openmrs.module.clinomix.dao.impl;

import org.hibernate.SessionFactory;
import org.openmrs.module.clinomix.dao.ClinomixDao;

/**
 * Hibernate implementation of {@link ClinomixDao}.
 */
public class ClinomixDaoImpl implements ClinomixDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
