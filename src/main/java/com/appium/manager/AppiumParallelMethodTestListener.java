package com.appium.manager;

import static com.google.common.base.Preconditions.checkNotNull;

import com.annotation.values.RetryCount;
import com.annotation.values.SkipIf;
import com.appium.capabilities.Capabilities;
import com.appium.device.GenyMotionManager;
import com.appium.utils.FileFilterParser;
import com.appium.utils.Helpers;
import com.context.SessionContext;
import com.context.TestExecutionContext;
import io.appium.java_client.AppiumDriver;
import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.SkipException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public final class AppiumParallelMethodTestListener extends Helpers
    implements ITestListener, IInvokedMethodListener, ISuiteListener {

    private static final Logger LOGGER = Logger.getLogger(
            AppiumParallelMethodTestListener.class.getName());
    private TestLogger testLogger;
    private DeviceAllocationManager deviceAllocationManager;
    private AppiumServerManager appiumServerManager;
    private AppiumDriverManager appiumDriverManager;
    private Optional<String> atdHost;
    private Optional<String> atdPort;
    private static ThreadLocal<ITestNGMethod> currentMethods = new ThreadLocal<>();
    private static ThreadLocal<HashMap<String, String>> testResults = new ThreadLocal<>();
    private List<ITestNGListener> listeners;

    public AppiumParallelMethodTestListener() {
        testLogger = new TestLogger();
        appiumServerManager = new AppiumServerManager();
        deviceAllocationManager = DeviceAllocationManager.getInstance();
        appiumDriverManager = new AppiumDriverManager();
        atdHost = Optional.ofNullable(Capabilities.getInstance()
            .getMongoDbHostAndPort().get("atdHost"));
        atdPort = Optional.ofNullable(Capabilities.getInstance()
            .getMongoDbHostAndPort().get("atdPort"));
        listeners = initialiseListeners();
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
     * Allocates Devices to each test method
     * Instantiate Android or IOS Driver Instance
     * Starts ADB, Video Logging
     * Set Description for each test method with platform and UDID allocated to it.
     */
    @Override
    public void onTestStart(ITestResult iTestResult) {

    }

    private boolean isCloudExecution() {
        return AppiumDeviceManager.getAppiumDevice().getDevice().isCloud();
    }

    private void startReportLogging(ITestResult iTestResult) throws IOException,
        InterruptedException {
        testLogger.startLogging(iTestResult);
    }

    /*
     * Skips execution based on platform
     */
    @Override
    public void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
        allocateDeviceAndStartDriver(iTestResult);
        if (!isCloudExecution()) {
            currentMethods.set(iInvokedMethod.getTestMethod());
            SkipIf annotation = iInvokedMethod.getTestMethod().getConstructorOrMethod().getMethod()
                .getAnnotation(SkipIf.class);
            if (annotation != null && AppiumDriverManager.getDriver().getPlatformName()
                .equalsIgnoreCase(annotation.platform())) {
                if (atdHost.isPresent() && atdPort.isPresent()) {
                    HashMap<String, String> logs = new HashMap<>();
                    String url = "http://" + atdHost.get() + ":" + atdPort.get() + "/testresults";
                    sendResultsToAtdService(iTestResult, "Completed", url, logs);
                }
                throw new SkipException("Skipped because property was set to :::"
                    + annotation.platform());
            }
        }
        TestExecutionContext testExecutionContext =
                new TestExecutionContext(iInvokedMethod.getTestMethod().getMethodName());
        testExecutionContext.addTestState("appiumDriver",AppiumDriverManager.getDriver());
        testExecutionContext.addTestState("deviceId",
                AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());

        queueBeforeInvocationListeners(iInvokedMethod, iTestResult, listeners);
    }

    private void allocateDeviceAndStartDriver(ITestResult iTestResult) {
        try {
            AppiumDriver driver = AppiumDriverManager.getDriver();
            if (driver == null || driver.getSessionId() == null) {
                deviceAllocationManager.allocateDevice(deviceAllocationManager
                    .getNextAvailableDevice());
                appiumDriverManager.startAppiumDriverInstance();
                if (!isCloudExecution()) {
                    startReportLogging(iTestResult);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Stops driver after each test method execution
     * De-allocates device after each test method execution
     * Terminate logs getting captured after each test method execution
     */
    @Override
    public void afterInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
        {
            try {
                if (!isCloudExecution() && !isRetry(iTestResult)) {
                    HashMap<String, String> logs = testLogger.endLogging(iTestResult,
                            AppiumDeviceManager.getAppiumDevice().getDevice().getDeviceModel());
                    if (atdHost.isPresent() && atdPort.isPresent()) {
                        String url = "http://" + atdHost.get() + ":" + atdPort.get() + "/testresults";
                        sendResultsToAtdService(iTestResult, "Completed", url, logs);
                    } else {
                        new FileFilterParser()
                                .getScreenShotPaths(AppiumDeviceManager.getAppiumDevice()
                                        .getDevice().getUdid(), iTestResult);
                        testResults.set(logs);
                    }
                }
                if (iInvokedMethod.isTestMethod()) {
                    AppiumDriverManager.getDriver().quit();
                    deviceAllocationManager.freeDevice();
                    if (!isCloudExecution()) {
                        appiumDriverManager.stopAppiumDriver();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            SessionContext.remove(Thread.currentThread().getId());
            queueAfterInvocationListener(iInvokedMethod, iTestResult, listeners);
        }
    }

    /*
     * Stops Appium Server after each test suite
     */
    @Override
    public void onFinish(ISuite iSuite) {
        if (Capabilities.getInstance()
            .getCapabilityObjectFromKey("genycloud") != null) {
            try {
                new GenyMotionManager().stopAllGenymotionInstances();
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        }
        try {
            appiumServerManager.stopAppiumServer();
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
        if (!isCloudExecution()) {
            LOGGER.info("In Skipped...");
            RetryCount retryCount = iTestResult.getMethod().getConstructorOrMethod()
                .getMethod().getAnnotation(RetryCount.class);
            if (retryCount == null && (atdHost.isPresent() && atdPort.isPresent())) {
                HashMap<String, String> logs = new HashMap<>();
                String url = "http://" + atdHost.get() + ":" + atdPort.get() + "/testresults";
                sendResultsToAtdService(iTestResult, "Completed", url, logs);
            }
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
