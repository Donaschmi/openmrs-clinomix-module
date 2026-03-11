package org.openmrs.module.clinomix.web.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.clinomix.api.StudyService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Non-CRUD REST endpoints for Studies:
 *
 *   POST   /ws/rest/v1/clinomix/study/{uuid}/response           — add a response to a study
 *   DELETE /ws/rest/v1/clinomix/study/{uuid}/response/{id}      — remove a response from a study
 *   GET    /ws/rest/v1/clinomix/study/{uuid}/export             — export study as FHIR Bundle JSON
 */
@Controller
@RequestMapping("/" + RestConstants.VERSION_1 + "/clinomix/study")
public class StudyController {

    private StudyService getService() {
        return Context.getService(StudyService.class);
    }

    /**
     * POST /ws/rest/v1/clinomix/study/{uuid}/response
     * Body: { "questionnaireResponseFhirId": "qr-001" }
     */
    @PostMapping(value = "/{uuid}/response",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> addResponse(
            @PathVariable("uuid") String studyUuid,
            @RequestBody Map<String, String> body) {

        String responseId = body.get("questionnaireResponseFhirId");
        if (responseId == null || responseId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        getService().addResponse(studyUuid, responseId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * DELETE /ws/rest/v1/clinomix/study/{uuid}/response/{id}
     * {id} is the internal DB id of the StudyResponse row.
     */
    @DeleteMapping("/{uuid}/response/{id}")
    @ResponseBody
    public ResponseEntity<Void> removeResponse(
            @PathVariable("uuid") String studyUuid,
            @PathVariable("id") Integer studyResponseId) {

        getService().removeResponse(studyUuid, studyResponseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /ws/rest/v1/clinomix/study/{uuid}/export
     * Returns a FHIR R4 Bundle containing all Questionnaires and
     * QuestionnaireResponses associated with the study.
     */
    @GetMapping(value = "/{uuid}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> exportStudy(@PathVariable("uuid") String studyUuid) {
        String bundle = getService().exportStudy(studyUuid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(bundle);
    }
}
