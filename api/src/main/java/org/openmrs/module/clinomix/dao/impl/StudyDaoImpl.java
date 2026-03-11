package org.openmrs.module.clinomix.dao.impl;

import org.hibernate.SessionFactory;
import org.openmrs.module.clinomix.dao.StudyDao;
import org.openmrs.module.clinomix.model.Study;
import org.openmrs.module.clinomix.model.StudyResponse;

import java.util.List;

/**
 * Hibernate implementation of {@link StudyDao}.
 */
public class StudyDaoImpl implements StudyDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Study saveStudy(Study study) {
        sessionFactory.getCurrentSession().saveOrUpdate(study);
        return study;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Study getStudyByUuid(String uuid) {
        return (Study) sessionFactory.getCurrentSession()
                .createQuery("from Study s where s.uuid = :uuid")
                .setParameter("uuid", uuid)
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Study> getAllStudies() {
        return sessionFactory.getCurrentSession()
                .createQuery("from Study s order by s.dateCreated desc")
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Study> getStudiesByStatus(String status) {
        return sessionFactory.getCurrentSession()
                .createQuery("from Study s where s.status = :status order by s.dateCreated desc")
                .setParameter("status", status)
                .list();
    }

    @Override
    public void deleteStudy(Study study) {
        sessionFactory.getCurrentSession().delete(study);
    }

    @Override
    public StudyResponse saveResponse(StudyResponse response) {
        sessionFactory.getCurrentSession().saveOrUpdate(response);
        return response;
    }

    @Override
    public StudyResponse getResponseById(Integer id) {
        return (StudyResponse) sessionFactory.getCurrentSession().get(StudyResponse.class, id);
    }

    @Override
    public void deleteResponse(StudyResponse response) {
        sessionFactory.getCurrentSession().delete(response);
    }
}
