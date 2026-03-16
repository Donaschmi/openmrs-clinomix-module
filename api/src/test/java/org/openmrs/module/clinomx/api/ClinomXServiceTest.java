package org.openmrs.module.clinomx.api;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ClinomXService}.
 */
public class ClinomXServiceTest extends BaseModuleContextSensitiveTest {

    @Autowired
    private ClinomXService clinomXService;

    @Test
    public void getModuleInfo_shouldReturnModuleInfo() {
        String info = clinomXService.getModuleInfo();
        Assert.assertNotNull(info);
        Assert.assertTrue(info.contains("ClinomX"));
    }
}
