package org.openmrs.module.clinomix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.ModuleFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * This class contains the logic that is run every time this module is either started or shutdown.
 */
public class ClinomixActivator implements ModuleActivator {

    private static final Log log = LogFactory.getLog(ClinomixActivator.class);

    /**
     * @see ModuleActivator#willRefreshContext()
     */
    @Override
    public void willRefreshContext() {
        log.info("Refreshing Clinomix Module");
    }

    /**
     * @see ModuleActivator#contextRefreshed()
     */
    @Override
    public void contextRefreshed() {
        log.info("Clinomix Module refreshed");
        try {
            seedDummyPatients();
        } catch (Exception e) {
            log.error("Failed to seed dummy patients", e);
        }
    }

    /**
     * @see ModuleActivator#willStart()
     */
    @Override
    public void willStart() {
        log.info("Starting Clinomix Module");
    }

    /**
     * @see ModuleActivator#started()
     */
    @Override
    public void started() {
        log.info("Clinomix Module started");
        try {
            seedDummyPatients();
        } catch (Exception e) {
            log.error("Failed to seed dummy patients", e);
        }
    }

    private void seedDummyPatients() {
        log.info("Seeding dummy patients...");
        PatientService patientService = Context.getPatientService();

        PatientIdentifierType identifierType = patientService.getAllPatientIdentifierTypes()
                .stream()
                .findFirst()
                .orElse(null);

        if (identifierType == null) {
            log.warn("No PatientIdentifierType found — cannot seed dummy patients");
            return;
        }
        log.info("Using identifier type: " + identifierType.getName());

        Location location = Context.getLocationService().getDefaultLocation();
        if (location == null) {
            location = Context.getLocationService().getAllLocations()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        if (location == null) {
            log.warn("No Location found — cannot seed dummy patients");
            return;
        }
        log.info("Using location: " + location.getName());

        List<String[]> patients = Arrays.asList(
                new String[]{"Alice", "Dummy", "F", "CLX-001"},
                new String[]{"Bob",   "Dummy", "M", "CLX-002"},
                new String[]{"Carol", "Dummy", "F", "CLX-003"}
        );

        for (String[] data : patients) {
            String identifier = data[3];
            try {
                // Skip if this identifier is already in use
                if (!patientService.getPatientIdentifiers(identifier, null, null, null, null).isEmpty()) {
                    log.info("Dummy patient with identifier " + identifier + " already exists — skipping");
                    continue;
                }

                Patient patient = new Patient();

                PersonName name = new PersonName();
                name.setGivenName(data[0]);
                name.setFamilyName(data[1]);
                patient.addName(name);

                patient.setGender(data[2]);
                patient.setBirthdate(new Date(0)); // 1970-01-01

                PatientIdentifier pid = new PatientIdentifier();
                pid.setIdentifier(identifier);
                pid.setIdentifierType(identifierType);
                pid.setLocation(location);
                pid.setPreferred(true);
                patient.addIdentifier(pid);

                patientService.savePatient(patient);
                log.info("Created dummy patient: " + data[0] + " " + data[1]);
            } catch (Exception e) {
                log.error("Failed to create dummy patient " + data[0] + " (" + identifier + ")", e);
            }
        }
    }

    /**
     * @see ModuleActivator#willStop()
     */
    @Override
    public void willStop() {
        log.info("Stopping Clinomix Module");
    }

    /**
     * @see ModuleActivator#stopped()
     */
    @Override
    public void stopped() {
        log.info("Clinomix Module stopped");
    }
}
