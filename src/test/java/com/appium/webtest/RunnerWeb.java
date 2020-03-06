package com.appium.webtest;

import java.util.ArrayList;
import java.util.List;

import com.appium.manager.ATDRunner;
import org.testng.annotations.Test;

public class RunnerWeb {
    @Test
    public void runWebTests() throws Exception {
        List<String> tests = new ArrayList<>();
        ATDRunner atdRunner = new ATDRunner();

        tests.add("LoginFailureTest");
        boolean hasFailures = atdRunner.runner("com.appium.webtest", tests);
    }
}