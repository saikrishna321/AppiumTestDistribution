package com.test.cucumber.stepdefinitions;

import java.io.IOException;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;

public class Hooks {
    @Before
    public void beforeClass(Scenario scenario) throws Exception {
        //Do Anything
    }

    @After
    public void afterClass(Scenario scenario) throws InterruptedException, IOException {

    }
}
