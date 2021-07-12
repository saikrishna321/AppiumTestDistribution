package com.context;

import org.apache.log4j.Logger;

import java.util.HashMap;

public class TestExecutionContext {
    private final String testName;
    private final HashMap<String, Object> testExecutionState;
    private final String NOT_SET = "NOT-YET-SET";
    private static final Logger LOGGER = Logger.getLogger(
            TestExecutionContext.class.getSimpleName());

    public TestExecutionContext(String testName) {
        SessionContext.addContext(Thread.currentThread().getId(), this);
        this.testName = testName;
        this.testExecutionState = new HashMap<>();
        LOGGER.info(String.format("%s - TestExecution context created", testName));
    }

    public String getTestName() {
        return testName;
    }

    public void addTestState(String key, Object details) {
        testExecutionState.put(key, details);
    }

    public Object getTestState(String key) {
        return testExecutionState.get(key);
    }

    public String getTestStateAsString(String key) {
        return (String) testExecutionState.get(key);
    }
}
