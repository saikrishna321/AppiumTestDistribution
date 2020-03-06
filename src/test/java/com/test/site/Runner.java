package com.test.site;

import java.util.ArrayList;
import java.util.List;

import com.appium.manager.ATDRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Runner {
    @Test
    public static void testApp() throws Exception {
        List<String> tests = new ArrayList<>();
        tests.add("SliderTest");
        tests.add("DragnDropTest");
        ATDRunner atdRunner = new ATDRunner();
        boolean hasFailures = atdRunner.runner("com.test.site", tests);
        Assert.assertFalse(hasFailures, "Testcases have failed in parallel execution");
    }
}