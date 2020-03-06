package com.appium.executor;

import org.testng.annotations.Test;

public class TestRunner2 {
    @Test
    public void testMethod1() {
        System.err.println("******RunnerTest1Method1****" + Thread.currentThread()
            .getId());
    }

    @Test
    public void testMethod2() {
        System.err.println("******RunnerTest1Method2****" + Thread.currentThread()
            .getId());
    }

    @Test
    public void testMethod3() {
        System.err.println("******RunnerTest1Method3****" + Thread.currentThread()
            .getId());
    }

    @Test
    public void testMethod4() {
        System.err.println("******Runner1TestMethod4****" + Thread.currentThread()
            .getId());
    }
}