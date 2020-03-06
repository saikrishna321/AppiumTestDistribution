package com.appium.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;

/**
 * Created by saikrisv on 22/06/17.
 */
public class RetryListener implements IAnnotationTransformer {
    @Override
    public void transform(ITestAnnotation iTestAnnotation, Class aClass, Constructor constructor,
        Method method) {
        Class<? extends IRetryAnalyzer> retry = iTestAnnotation.getRetryAnalyzerClass();
        if (retry == null) {
            iTestAnnotation.setRetryAnalyzer(Retry.class);
        }
    }
}