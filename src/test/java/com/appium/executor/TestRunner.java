package com.appium.executor;

import org.testng.annotations.Test;

public class TestRunner {
    @Test
    public void testMethod1() {
        System.err.println("******RunnerTestMethod1****" + Thread.currentThread()
            .getId());
    }

    @Test
    public void testMethod2() {
        System.err.println("******RunnerTestMethod2****" + Thread.currentThread()
            .getId());
    }

    @Test
    public void testMethod3() {
        System.err.println("******RunnerTestMethod3****" + Thread.currentThread()
            .getId());
    }

    @Test
    public void testMethod4() {
        System.err.println("******RunnerTestMethod4****" + Thread.currentThread()
            .getId());
    }
}