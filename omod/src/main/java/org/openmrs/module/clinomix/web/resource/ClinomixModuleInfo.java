package org.openmrs.module.clinomix.web.resource;

/**
 * Simple DTO used as the delegate type for {@link ClinomixResource}.
 */
public class ClinomixModuleInfo {

    private String info;

    public ClinomixModuleInfo() {
    }

    public ClinomixModuleInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
