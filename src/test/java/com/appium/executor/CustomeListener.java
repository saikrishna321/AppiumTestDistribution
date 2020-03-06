package com.appium.executor;

import com.github.manager.AppiumDeviceManager;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public class CustomeListener implements IInvokedMethodListener {

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        String msg = String.format("%s.afterInvocation() was invoked", getClass().getName());
        System.out.println("After DeviceId" + AppiumDeviceManager.getAppiumDevice()
            .getDevice()
            .getUdid());
        System.err.println(msg);
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        String msg = String.format("%s.beforeInvocation() was invoked", getClass().getName());
        System.out.println("Before DeviceId" + AppiumDeviceManager.getAppiumDevice()
            .getDevice()
            .getUdid());
        System.err.println(msg);
    }
}