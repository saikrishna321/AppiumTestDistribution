package com.cucumber.listener;

import com.appium.capabilities.Capabilities;
import com.appium.filelocations.FileLocations;
import com.appium.manager.ATDRunner;
import com.appium.manager.AppiumDevice;
import com.appium.manager.AppiumDeviceManager;
import com.appium.manager.AppiumDriverManager;
import com.appium.manager.AppiumServerManager;
import com.appium.manager.DeviceAllocationManager;
import com.context.SessionContext;
import com.context.TestExecutionContext;
import com.epam.reportportal.service.ReportPortal;
import com.github.device.Device;
import io.appium.java_client.AppiumDriver;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CucumberScenarioListener implements ConcurrentEventListener {
    private static final Logger LOGGER = Logger.getLogger(CucumberScenarioListener.class.getName());
    private final AppiumDriverManager appiumDriverManager;
    private final DeviceAllocationManager deviceAllocationManager;
    private final AppiumServerManager appiumServerManager;
    private final Optional<String> atdHost;
    private final Optional<String> atdPort;
    private Map<String, Integer> scenarioRunCounts = new HashMap<String, Integer>();

    public CucumberScenarioListener() {
        LOGGER.info(String.format("ThreadID: %d: CucumberScenarioListener\n",
                Thread.currentThread().getId()));
        new ATDRunner();
        appiumServerManager = new AppiumServerManager();
        deviceAllocationManager = DeviceAllocationManager.getInstance();
        appiumDriverManager = new AppiumDriverManager();
        atdHost =
                Optional.ofNullable(Capabilities.getInstance()
                        .getMongoDbHostAndPort().get("atdHost"));
        atdPort =
                Optional.ofNullable(Capabilities.getInstance()
                        .getMongoDbHostAndPort().get("atdPort"));
    }

    private AppiumDevice allocateDeviceAndStartDriver() {
        try {
            AppiumDriver driver = AppiumDriverManager.getDriver();
            AppiumDevice availableDevice = deviceAllocationManager.getNextAvailableDevice();
            deviceAllocationManager.allocateDevice(availableDevice);
            if (driver == null || driver.getSessionId() == null) {
                appiumDriverManager.startAppiumDriverInstance();
            }
            return updateAvailableDeviceInformation(availableDevice);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private AppiumDevice updateAvailableDeviceInformation(AppiumDevice availableDevice) {
        org.openqa.selenium.Capabilities capabilities = AppiumDriverManager.getDriver()
                .getCapabilities();
        LOGGER.info("allocateDeviceAndStartDriver: "
                + capabilities);

        String udid = capabilities.is("udid")
                              ? getCapabilityFor(capabilities, "udid")
                              : getCapabilityFor(capabilities, "deviceUDID");
        Device device = availableDevice.getDevice();
        device.setUdid(udid);
        device.setDeviceManufacturer(
                getCapabilityFor(capabilities, "deviceManufacturer"));
        device.setDeviceModel(
                getCapabilityFor(capabilities, "deviceModel"));
        device.setName(
                getCapabilityFor(capabilities, "deviceName")
                        + " "
                        + getCapabilityFor(capabilities, "deviceModel"));
        device.setApiLevel(
                getCapabilityFor(capabilities, "deviceApiLevel"));
        device.setDeviceType(
                getCapabilityFor(capabilities, "platformName"));
        device.setScreenSize(
                getCapabilityFor(capabilities, "deviceScreenSize"));
        return availableDevice;
    }

    private String getCapabilityFor(org.openqa.selenium.Capabilities capabilities, String name) {
        Object capability = capabilities.getCapability(name);
        return null == capability ? "" : capability.toString();
    }

    private boolean isCloudExecution() {
        return AppiumDeviceManager.getAppiumDevice().getDevice().isCloud();
    }

    @Override
    public void setEventPublisher(EventPublisher eventPublisher) {
        eventPublisher.registerHandlerFor(TestRunStarted.class, this::runStartedHandler);
        eventPublisher.registerHandlerFor(TestCaseStarted.class, this::caseStartedHandler);
        eventPublisher.registerHandlerFor(TestCaseFinished.class, this::caseFinishedHandler);
        eventPublisher.registerHandlerFor(TestRunFinished.class, this::runFinishedHandler);
    }

    private void runStartedHandler(TestRunStarted event) {
        LOGGER.info("runStartedHandler");
        LOGGER.info(String.format("ThreadID: %d: beforeSuite: \n", Thread.currentThread().getId()));
        try {
            appiumServerManager.startAppiumServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void caseStartedHandler(TestCaseStarted event) {
        String scenarioName = event.getTestCase().getName();
        LOGGER.info("$$$$$   TEST-CASE  -- " + scenarioName + "  STARTED   $$$$$");
        LOGGER.info("caseStartedHandler: " + scenarioName);
        Integer scenarioRunCount = getScenarioRunCount(scenarioName);
        String normalisedScenarioName = normaliseScenarioName(scenarioName);
        LOGGER.info(
                String.format("ThreadID: %d: beforeScenario: for scenario: %s\n",
                        Thread.currentThread().getId(), scenarioName));
        AppiumDevice allocatedDevice = allocateDeviceAndStartDriver();
        String deviceLogFileName = null;

        try {
            deviceLogFileName =
                    allocatedDevice.startDataCapture(normalisedScenarioName, scenarioRunCount);
        } catch (IOException | InterruptedException e) {
            LOGGER.info("Error in starting data capture: " + e.getMessage());
            e.printStackTrace();
        }
        if (!isCloudExecution()) {
            if (atdHost.isPresent() && atdPort.isPresent()) {
                HashMap<String, String> logs = new HashMap<>();
                String url = "http://" + atdHost.get() + ":" + atdPort.get() + "/testresults";
            }
        }

        TestExecutionContext testExecutionContext = new TestExecutionContext(scenarioName);
        testExecutionContext.addTestState("appiumDriver",AppiumDriverManager.getDriver());
        testExecutionContext.addTestState("deviceId",
                AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
        testExecutionContext.addTestState("deviceInfo", allocatedDevice);
        testExecutionContext.addTestState("deviceLog", deviceLogFileName);
        testExecutionContext.addTestState("scenarioDirectory", FileLocations.REPORTS_DIRECTORY
                + normalisedScenarioName);
        testExecutionContext.addTestState("scenarioScreenshotsDirectory",
                FileLocations.REPORTS_DIRECTORY
                        + normalisedScenarioName
                        + File.separator
                        + "screenshot"
                        + File.separator);
    }

    private boolean isRunningOnpCloudy(AppiumDevice allocatedDevice) {
        boolean isPCloudy = Capabilities.getInstance()
                .getCloudName(allocatedDevice.getHostName()) == null ? false : true;
        LOGGER.info(allocatedDevice.getDevice().getName() + " running on pCloudy? " + isPCloudy);
        return isPCloudy;
    }

    private String normaliseScenarioName(String scenarioName) {
        return scenarioName.replaceAll("[`~ !@#$%^&*()\\-=+\\[\\]{}\\\\|;:'\",<.>/?]", "_");
    }

    private Integer getScenarioRunCount(String scenarioName) {
        if (scenarioRunCounts.containsKey(scenarioName)) {
            scenarioRunCounts.put(scenarioName, scenarioRunCounts.get(scenarioName) + 1);
        } else {
            scenarioRunCounts.put(scenarioName, 1);
        }
        return scenarioRunCounts.get(scenarioName);
    }

    private void caseFinishedHandler(TestCaseFinished event) {
        String scenarioName = event.getTestCase().getName();
        LOGGER.info("caseFinishedHandler Name: " + scenarioName);
        LOGGER.info("caseFinishedHandler Result: " + event.getResult().getStatus().toString());
        long threadId = Thread.currentThread().getId();
        LOGGER.info(
                String.format("ThreadID: %d: afterScenario: for scenario: %s\n",
                        threadId, event.getTestCase().toString()));

        TestExecutionContext testExecutionContext =
                SessionContext.getTestExecutionContext(threadId);

        AppiumDriver driver = (AppiumDriver) testExecutionContext.getTestState("appiumDriver");
        AppiumDevice allocatedDevice = (AppiumDevice) testExecutionContext
                .getTestState("deviceInfo");
        if (isRunningOnpCloudy(allocatedDevice)) {
            String link = (String) driver.executeScript("pCloudy_getReportLink");
            String message = "pCloudy Mobilab Report link available here: " + link;
            LOGGER.info(message);
            ReportPortal.emitLog(message, "DEBUG", new Date());
        }

        deviceAllocationManager.freeDevice();
        try {
            appiumDriverManager.stopAppiumDriver();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String deviceLogFileName = testExecutionContext.getTestStateAsString("deviceLog");
        if (null != deviceLogFileName) {
            // deviceLogFileName may be null for non-Native Android tests
            LOGGER.info("deviceLogFileName: " + deviceLogFileName);
            ReportPortal.emitLog("ADB Logs", "DEBUG", new Date(), new File(deviceLogFileName));
        }
        SessionContext.remove(threadId);
        LOGGER.info("$$$$$   TEST-CASE  -- " + scenarioName + "  ENDED   $$$$$");
    }

    private void runFinishedHandler(TestRunFinished event) {
        LOGGER.info("runFinishedHandler: " + event.getResult().toString());
        LOGGER.info(String.format("ThreadID: %d: afterSuite: \n", Thread.currentThread().getId()));
        try {
            appiumServerManager.stopAppiumServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
