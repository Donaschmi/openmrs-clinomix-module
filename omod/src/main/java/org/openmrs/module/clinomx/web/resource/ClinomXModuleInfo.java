package org.openmrs.module.clinomx.web.resource;

/**
 * Simple DTO used as the delegate type for {@link ClinomXResource}.
 */
public class ClinomXModuleInfo {

    private String info;

    public ClinomXModuleInfo() {
    }

    public ClinomXModuleInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
