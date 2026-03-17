package org.openmrs.module.clinomx.dao.impl;

import org.hibernate.SessionFactory;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.openmrs.module.clinomx.dao.ClinomXDao;
import org.openmrs.module.clinomx.model.QuestionnaireRecord;
import org.openmrs.module.clinomx.model.QuestionnaireResponseRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Native-SQL implementation of {@link ClinomXDao}.
 * Uses raw SQL to avoid Hibernate entity registration (no HBM/annotation mapping needed).
 */
public class ClinomXDaoImpl implements ClinomXDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private org.hibernate.Session session() {
        return sessionFactory.getCurrentSession();
    }

    private QuestionnaireRecord mapQuestionnaire(Map<String, Object> row) {
        QuestionnaireRecord r = new QuestionnaireRecord();
        r.setId(toInteger(row.get("questionnaire_id")));
        r.setUuid((String) row.get("uuid"));
        r.setFhirId((String) row.get("fhir_id"));
        r.setTitle((String) row.get("title"));
        r.setStatus((String) row.get("status"));
        r.setFhirJson((String) row.get("fhir_json"));
        r.setDateCreated((Date) row.get("date_created"));
        r.setDateChanged((Date) row.get("date_changed"));
        return r;
    }

    private QuestionnaireResponseRecord mapResponse(Map<String, Object> row) {
        QuestionnaireResponseRecord r = new QuestionnaireResponseRecord();
        r.setId(toInteger(row.get("response_id")));
        r.setUuid((String) row.get("uuid"));
        r.setFhirId((String) row.get("fhir_id"));
        r.setQuestionnaireId(toInteger(row.get("questionnaire_id")));
        r.setPatientUuid((String) row.get("patient_uuid"));
        r.setStatus((String) row.get("status"));
        r.setAuthored((Date) row.get("authored"));
        r.setFhirJson((String) row.get("fhir_json"));
        r.setDateCreated((Date) row.get("date_created"));
        return r;
    }

    private Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Integer) return (Integer) o;
        return ((Number) o).intValue();
    }

    @SuppressWarnings("unchecked")
    private List<QuestionnaireRecord> queryQuestionnaires(String sql, Object... params) {
        org.hibernate.Query q = session().createSQLQuery(sql)
                .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        for (int i = 0; i < params.length; i++) q.setParameter(i, params[i]);
        List<Map<String, Object>> rows = q.list();
        List<QuestionnaireRecord> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) result.add(mapQuestionnaire(row));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<QuestionnaireResponseRecord> queryResponses(String sql, Object... params) {
        org.hibernate.Query q = session().createSQLQuery(sql)
                .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        for (int i = 0; i < params.length; i++) q.setParameter(i, params[i]);
        List<Map<String, Object>> rows = q.list();
        List<QuestionnaireResponseRecord> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) result.add(mapResponse(row));
        return result;
    }

    // ── Questionnaire ──────────────────────────────────────────────────────────

    @Override
    public QuestionnaireRecord saveQuestionnaire(QuestionnaireRecord q) {
        if (q.getId() == null) {
            session().createSQLQuery(
                    "INSERT INTO clinom_x_questionnaire (uuid, fhir_id, title, status, fhir_json, date_created, date_changed) " +
                    "VALUES (:uuid, :fhirId, :title, :status, :fhirJson, :dateCreated, :dateChanged)")
                .setString("uuid", q.getUuid())
                .setString("fhirId", q.getFhirId())
                .setString("title", q.getTitle())
                .setString("status", q.getStatus())
                .setString("fhirJson", q.getFhirJson())
                .setTimestamp("dateCreated", q.getDateCreated())
                .setTimestamp("dateChanged", q.getDateChanged())
                .executeUpdate();
            Integer id = ((Number) session().createSQLQuery(
                    "SELECT questionnaire_id FROM clinom_x_questionnaire WHERE uuid = :uuid")
                .setString("uuid", q.getUuid())
                .uniqueResult()).intValue();
            q.setId(id);
        } else {
            session().createSQLQuery(
                    "UPDATE clinom_x_questionnaire SET fhir_id=:fhirId, title=:title, status=:status, " +
                    "fhir_json=:fhirJson, date_changed=:dateChanged WHERE questionnaire_id=:id")
                .setString("fhirId", q.getFhirId())
                .setString("title", q.getTitle())
                .setString("status", q.getStatus())
                .setString("fhirJson", q.getFhirJson())
                .setTimestamp("dateChanged", q.getDateChanged())
                .setInteger("id", q.getId())
                .executeUpdate();
        }
        return q;
    }

    @Override
    public QuestionnaireRecord getQuestionnaireByUuid(String uuid) {
        List<QuestionnaireRecord> list = queryQuestionnaires(
                "SELECT * FROM clinom_x_questionnaire WHERE uuid = ?", uuid);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public QuestionnaireRecord getQuestionnaireByFhirId(String fhirId) {
        List<QuestionnaireRecord> list = queryQuestionnaires(
                "SELECT * FROM clinom_x_questionnaire WHERE fhir_id = ?", fhirId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<QuestionnaireRecord> getAllQuestionnaires() {
        return queryQuestionnaires("SELECT * FROM clinom_x_questionnaire ORDER BY questionnaire_id");
    }

    @Override
    public List<QuestionnaireRecord> searchQuestionnairesByTitle(String title) {
        return queryQuestionnaires(
                "SELECT * FROM clinom_x_questionnaire WHERE LOWER(title) LIKE LOWER(?)",
                "%" + title + "%");
    }

    @Override
    public void deleteQuestionnaire(QuestionnaireRecord questionnaire) {
        session().createSQLQuery("DELETE FROM clinom_x_questionnaire WHERE questionnaire_id = ?")
                .setInteger(0, questionnaire.getId())
                .executeUpdate();
    }

    // ── QuestionnaireResponse ──────────────────────────────────────────────────

    @Override
    public QuestionnaireResponseRecord saveQuestionnaireResponse(QuestionnaireResponseRecord r) {
        if (r.getId() == null) {
            session().createSQLQuery(
                    "INSERT INTO clinom_x_questionnaire_response " +
                    "(uuid, fhir_id, questionnaire_id, patient_uuid, status, authored, fhir_json, date_created) " +
                    "VALUES (:uuid, :fhirId, :questionnaireId, :patientUuid, :status, :authored, :fhirJson, :dateCreated)")
                .setString("uuid", r.getUuid())
                .setString("fhirId", r.getFhirId())
                .setParameter("questionnaireId", r.getQuestionnaireId())
                .setString("patientUuid", r.getPatientUuid())
                .setString("status", r.getStatus())
                .setTimestamp("authored", r.getAuthored())
                .setString("fhirJson", r.getFhirJson())
                .setTimestamp("dateCreated", r.getDateCreated())
                .executeUpdate();
            Integer id = ((Number) session().createSQLQuery(
                    "SELECT response_id FROM clinom_x_questionnaire_response WHERE uuid = :uuid")
                .setString("uuid", r.getUuid())
                .uniqueResult()).intValue();
            r.setId(id);
        } else {
            session().createSQLQuery(
                    "UPDATE clinom_x_questionnaire_response SET fhir_id=:fhirId, questionnaire_id=:questionnaireId, " +
                    "patient_uuid=:patientUuid, status=:status, authored=:authored, fhir_json=:fhirJson " +
                    "WHERE response_id=:id")
                .setString("fhirId", r.getFhirId())
                .setParameter("questionnaireId", r.getQuestionnaireId())
                .setString("patientUuid", r.getPatientUuid())
                .setString("status", r.getStatus())
                .setTimestamp("authored", r.getAuthored())
                .setString("fhirJson", r.getFhirJson())
                .setInteger("id", r.getId())
                .executeUpdate();
        }
        return r;
    }

    @Override
    public QuestionnaireResponseRecord getQuestionnaireResponseByUuid(String uuid) {
        List<QuestionnaireResponseRecord> list = queryResponses(
                "SELECT * FROM clinom_x_questionnaire_response WHERE uuid = ?", uuid);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<QuestionnaireResponseRecord> getAllQuestionnaireResponses() {
        return queryResponses("SELECT * FROM clinom_x_questionnaire_response ORDER BY response_id");
    }

    @Override
    public List<QuestionnaireResponseRecord> getResponsesByQuestionnaire(String questionnaireUuid) {
        return queryResponses(
                "SELECT r.* FROM clinom_x_questionnaire_response r " +
                "JOIN clinom_x_questionnaire q ON r.questionnaire_id = q.questionnaire_id " +
                "WHERE q.uuid = ?", questionnaireUuid);
    }

    @Override
    public List<QuestionnaireResponseRecord> getResponsesByPatient(String patientUuid) {
        return queryResponses(
                "SELECT * FROM clinom_x_questionnaire_response WHERE patient_uuid = ?", patientUuid);
    }

    @Override
    public List<QuestionnaireResponseRecord> getResponsesByQuestionnaireAndPatient(String questionnaireUuid,
                                                                                    String patientUuid) {
        return queryResponses(
                "SELECT r.* FROM clinom_x_questionnaire_response r " +
                "JOIN clinom_x_questionnaire q ON r.questionnaire_id = q.questionnaire_id " +
                "WHERE q.uuid = ? AND r.patient_uuid = ?", questionnaireUuid, patientUuid);
    }

    @Override
    public void deleteQuestionnaireResponse(QuestionnaireResponseRecord response) {
        session().createSQLQuery("DELETE FROM clinom_x_questionnaire_response WHERE response_id = ?")
                .setInteger(0, response.getId())
                .executeUpdate();
    }
}
