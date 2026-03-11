package org.openmrs.module.clinomix.api;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ClinomixService}.
 */
public class ClinomixServiceTest extends BaseModuleContextSensitiveTest {

    @Autowired
    private ClinomixService clinomixService;

    @Test
    public void getModuleInfo_shouldReturnModuleInfo() {
        String info = clinomixService.getModuleInfo();
        Assert.assertNotNull(info);
        Assert.assertTrue(info.contains("Clinomix"));
    }
}
