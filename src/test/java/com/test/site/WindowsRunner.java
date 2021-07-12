package com.test.site;

import com.appium.manager.ATDRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class WindowsRunner {
    @Test
    public static void testApp() throws Exception {
        List<String> tests = new ArrayList<>();
        tests.add("WindowsTest");
        System.setProperty("Platform", "windows");
        System.setProperty("CONFIG_FILE", "./configs/windowsconfig.properties");
        System.setProperty("CAPS", "./caps/windows.json");
        ATDRunner atdRunner = new ATDRunner();
        boolean hasFailures = atdRunner.runner("com.test.site", tests);
        Assert.assertFalse(hasFailures, "Testcases have failed in parallel execution");
    }
}
