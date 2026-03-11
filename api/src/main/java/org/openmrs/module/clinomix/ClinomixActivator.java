package org.openmrs.module.clinomix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ModuleActivator;

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
