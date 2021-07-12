package com.appium.manager;


import com.annotation.values.SkipIf;
import com.appium.capabilities.Capabilities;
import com.appium.device.DevicesByHost;
import com.appium.utils.Helpers;
import com.appium.device.HostMachineDeviceManager;
import com.context.SessionContext;
import com.context.TestExecutionContext;
import org.json.JSONObject;

import org.testng.IClassListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestClass;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.ITestNGListener;

import java.util.HashMap;
import java.util.Optional;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;


public final class AppiumParallelTestListener extends Helpers
    implements IClassListener, IInvokedMethodListener, ISuiteListener, ITestListener {

    private TestLogger testLogger;
    private DeviceAllocationManager deviceAllocationManager;
    private AppiumServerManager appiumServerManager;
    private AppiumDriverManager appiumDriverManager;
    private Optional<String> atdHost;
    private Optional<String> atdPort;
    private static ThreadLocal<ITestNGMethod> currentMethods = new ThreadLocal<>();
    List<ITestNGListener> iTestNGListeners;

    public AppiumParallelTestListener() throws Exception {
        testLogger = new TestLogger();
        appiumServerManager = new AppiumServerManager();
        deviceAllocationManager = DeviceAllocationManager.getInstance();
        appiumDriverManager = new AppiumDriverManager();
        atdHost = Optional.ofNullable(Capabilities.getInstance()
            .getMongoDbHostAndPort().get("atdHost"));
        atdPort = Optional.ofNullable(Capabilities.getInstance()
            .getMongoDbHostAndPort().get("atdPort"));
        iTestNGListeners = initialiseListeners();
    }


    /*
     * Handle Skipif annotation
     * SendResults to ATD service if required
     */
    @Override
    public void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult testResult) {
        currentMethods.set(iInvokedMethod.getTestMethod());
        SkipIf annotation = iInvokedMethod.getTestMethod().getConstructorOrMethod().getMethod()
            .getAnnotation(SkipIf.class);
        if (annotation != null && AppiumDriverManager.getDriver().getPlatformName()
            .equalsIgnoreCase(annotation.platform())) {
            throw new SkipException("Skipped because property was set to :::"
                + annotation.platform());
        }
        TestExecutionContext testExecutionContext =
                new TestExecutionContext(iInvokedMethod.getTestMethod().getMethodName());
        testExecutionContext.addTestState("appiumDriver",AppiumDriverManager.getDriver());
        testExecutionContext.addTestState("deviceId",
                AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
        queueBeforeInvocationListeners(iInvokedMethod, testResult, iTestNGListeners);
    }

    /*
     * Send results to ATD service if required
     * Stop Appium Driver after method invocation completed
     */
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        JSONObject json = new JSONObject();
        json.put("id", AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
        json.put("version", new AppiumDeviceManager().getDeviceVersion());
        json.put("platform", AppiumDeviceManager.getMobilePlatform());
        json.put("model", new AppiumDeviceManager().getDeviceModel());
        try {
            if (testResult.getStatus() == ITestResult.SUCCESS
                || testResult.getStatus() == ITestResult.FAILURE) {
                HashMap<String, String> logs = testLogger.endLogging(testResult,
                    AppiumDeviceManager.getAppiumDevice().getDevice().getDeviceModel());
                if (atdHost.isPresent() && atdPort.isPresent()) {
                    String postTestResults = "http://" + atdHost.get() + ":" + atdPort.get() + "/testresults";
                    sendResultsToAtdService(testResult, "Completed", postTestResults, logs);
                }
            }
            if (method.isTestMethod()) {
                appiumDriverManager.stopAppiumDriver();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SessionContext.remove(Thread.currentThread().getId());
        queueAfterInvocationListener(method, testResult, iTestNGListeners);
    }


    /*
     * Starts Appium Server before each test suite
     */
    @Override
    public void onStart(ISuite iSuite) {
        try {
            appiumServerManager.startAppiumServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Stops Appium Server after each test suite
     */
    @Override
    public void onFinish(ISuite iSuite) {
        try {
            appiumServerManager.stopAppiumServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Allocates device for test execution
     */
    @Override
    public void onBeforeClass(ITestClass testClass) {
        try {
            String device = testClass.getXmlClass().getAllParameters().get("device");
            String hostName = testClass.getXmlClass().getAllParameters().get("hostName");
            DevicesByHost devicesByHost = HostMachineDeviceManager.getInstance().getDevicesByHost();
            AppiumDevice appiumDevice = devicesByHost.getAppiumDevice(device, hostName);
            deviceAllocationManager.allocateDevice(appiumDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Deallocated device post test execution
     */
    @Override
    public void onAfterClass(ITestClass iTestClass) {
        deviceAllocationManager.freeDevice();
    }

    /*
     * Sets description for each test method with platform and Device UDID allocated to it.
     * Starts Driver instance for execution
     */
    @Override
    public void onTestStart(ITestResult iTestResult) {
        try {
            appiumDriverManager.startAppiumDriverInstance();
            testLogger.startLogging(iTestResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Document to make codacy happy
     */
    @Override
    public void onTestSuccess(ITestResult iTestResult) {

    }

    /*
     * Document to make codacy happy
     */
    @Override
    public void onTestFailure(ITestResult iTestResult) {

    }

    /*
     * Document to make codacy happy
     */
    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        if (atdHost.isPresent() && atdPort.isPresent()) {
            String url = "http://" + atdHost.get() + ":" + atdPort.get() + "/testresults";
            sendResultsToAtdService(iTestResult, "Skipped", url, new HashMap<>());
        }
    }

    /*
     * Document to make codacy happy
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    /*
     * Document to make codacy happy
     */
    @Override
    public void onStart(ITestContext iTestContext) {

    }

    /*
     * Document to make codacy happy
     */
    @Override
    public void onFinish(ITestContext iTestContext) {
        SessionContext.setReportPortalLaunchURL(iTestContext);
    }

    public static ITestNGMethod getTestMethod() {
        return checkNotNull(currentMethods.get(),
            "Did you forget to register the %s listener?",
            AppiumParallelMethodTestListener.class.getName());
    }
}
