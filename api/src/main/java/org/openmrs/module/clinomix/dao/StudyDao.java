package org.openmrs.module.clinomix.dao;

import org.openmrs.module.clinomix.model.Study;
import org.openmrs.module.clinomix.model.StudyResponse;

import java.util.List;

/**
 * Database access interface for {@link Study} and {@link StudyResponse}.
 */
public interface StudyDao {

    Study saveStudy(Study study);

    Study getStudyByUuid(String uuid);

    List<Study> getAllStudies();

    List<Study> getStudiesByStatus(String status);

    void deleteStudy(Study study);

    StudyResponse saveResponse(StudyResponse response);

    StudyResponse getResponseById(Integer id);

    void deleteResponse(StudyResponse response);
}
