package com.cucumber.listener;

import static java.lang.System.getProperty;
import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.appium.filelocations.FileLocations;
import com.appium.manager.DeviceSingleton;
import com.github.android.AndroidDeviceConfiguration;
import com.github.ios.IOSDeviceConfiguration;
import com.github.manager.AppiumDeviceManager;
import com.github.manager.AppiumDriverManager;
import com.github.manager.AppiumServerManager;
import com.github.manager.DeviceAllocationManager;
import com.github.utils.ImageUtils;
import com.github.utils.MobilePlatform;
import com.video.recorder.XpathXML;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import org.apache.commons.io.FileUtils;
import org.im4java.core.IM4JavaException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Cucumber custom format listener which generates ExtentsReport html file
 */
public class CucumberListener implements Reporter, Formatter, ISuiteListener {

    private final DeviceAllocationManager     deviceAllocationManager;
    public        AppiumServerManager         appiumServerManager;
    public        AppiumDriverManager         appiumDriverManager;
    public        DeviceSingleton             deviceSingleton;
    public        LinkedList<Step>            testSteps;
    public        AppiumDriver<MobileElement> appium_driver;
    private       AndroidDeviceConfiguration  androidDevice;
    private       IOSDeviceConfiguration      iosDevice;
    public        String                      deviceModel;
    public        ImageUtils                  imageUtils  = new ImageUtils();
    public        XpathXML                    xpathXML    = new XpathXML();
    private       String                      CI_BASE_URI = null;

    private static final Map<String, String> MIME_TYPES_EXTENSIONS = new HashMap() {
        {
            this.put("image/bmp", "bmp");
            this.put("image/gif", "gif");
            this.put("image/jpeg", "jpg");
            this.put("image/png", "png");
            this.put("image/svg+xml", "svg");
            this.put("video/ogg", "ogg");
        }
    };

    public CucumberListener() throws Exception {
        appiumServerManager = new AppiumServerManager();
        appiumDriverManager = new AppiumDriverManager();
        deviceAllocationManager = DeviceAllocationManager.getInstance();
        deviceSingleton = DeviceSingleton.getInstance();
        iosDevice = new IOSDeviceConfiguration();
        androidDevice = new AndroidDeviceConfiguration();
    }

    public void before(Match match, Result result) {
    }

    public void result(Result result) {
        if ("failed".equals(result.getStatus())) {
            String failed_StepName = testSteps.poll()
                .getName();
            String context = AppiumDriverManager.getDriver()
                .getContext();
            boolean contextChanged = false;
            if ("Android".equalsIgnoreCase(AppiumDriverManager.getDriver()
                .getSessionDetails()
                .get("platform")
                .toString()) && !"NATIVE_APP".equals(context)) {
                AppiumDriverManager.getDriver()
                    .context("NATIVE_APP");
                contextChanged = true;
            }
            File scrFile = ((TakesScreenshot) AppiumDriverManager.getDriver()).getScreenshotAs(
                OutputType.FILE);
            if (contextChanged) {
                AppiumDriverManager.getDriver()
                    .context(context);
            }
            if (AppiumDeviceManager.getMobilePlatform()
                .equals(MobilePlatform.ANDROID)) {
                deviceModel = androidDevice.getDeviceModel();
                screenShotAndFrame(failed_StepName, scrFile, "android");
            } else if (AppiumDeviceManager.getMobilePlatform()
                .equals(MobilePlatform.IOS)) {
                deviceModel = AppiumDeviceManager.getAppiumDevice()
                    .getDevice()
                    .getDeviceModel();
                screenShotAndFrame(failed_StepName, scrFile, "iPhone");
            }
            try {
                attachScreenShotToReport(failed_StepName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void after(Match match, Result result) {

    }

    public void match(Match match) {

    }

    public void embedding(String s, byte[] bytes) {
    }

    public void write(String s) {
        // ReportManager.endTest(parent);
    }

    public void syntaxError(String s, String s1, List<String> list, String s2, Integer integer) {

    }

    public void uri(String s) {

    }

    public void feature(Feature feature) {
        String[] tagsArray = getTagArray(feature.getTags());
        String tags = String.join(",", tagsArray);
        deviceAllocationManager.allocateDevice(deviceAllocationManager.getNextAvailableDevice());
    }

    private String[] getTagArray(List<Tag> tags) {
        String[] tagArray = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            tagArray[i] = tags.get(i)
                .getName();
        }
        return tagArray;
    }

    public void scenarioOutline(ScenarioOutline scenarioOutline) {

    }

    public void examples(Examples examples) {

    }

    public void startOfScenarioLifeCycle(Scenario scenario) {
        createAppiumInstance(scenario);
        this.testSteps = new LinkedList<Step>();
        System.out.println(testSteps);
    }

    public void createAppiumInstance(Scenario scenario) {
        String[] tagsArray = getTagArray(scenario.getTags());
        String tags = String.join(",", tagsArray);
        try {
            startAppiumServer(scenario, tags);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TO DO fix this
    public void startAppiumServer(Scenario scenario, String tags) throws Exception {
        appiumDriverManager.startAppiumDriverInstance();
        ///This portion should be Broken : TODO
    }

    public void background(Background background) {

    }

    public void scenario(Scenario scenario) {

    }

    public void step(Step step) {
        testSteps.add(step);
    }

    public void endOfScenarioLifeCycle(Scenario scenario) {
        AppiumDriverManager.getDriver()
            .quit();
    }

    public void done() {

    }

    public void close() {

    }

    public void eof() {
        try {
            deviceAllocationManager.freeDevice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void screenShotAndFrame(String failed_StepName, File scrFile, String device) {
        try {
            File framePath = new File(getProperty("user.dir") + "/src/test/resources/frames/");
            FileUtils.copyFile(scrFile, new File(
                format("{0}{1}{2}/{3}/{4}/failed_{5}.jpeg", getProperty("user.dir"),
                    FileLocations.SCREENSHOTS_DIRECTORY, device,
                    AppiumDeviceManager.getAppiumDevice()
                        .getDevice()
                        .getUdid(), deviceModel, failed_StepName.replaceAll(" ", "_"))));
            File[] files1 = framePath.listFiles();
            if (framePath.exists()) {
                for (File file : files1) {
                    if (file.isFile()) { //this line weeds out other directories/folders
                        Path p = Paths.get(file.toString());
                        String fileName = p.getFileName()
                            .toString()
                            .toLowerCase();
                        if (deviceModel.toString()
                            .toLowerCase()
                            .contains(fileName.split(".png")[0].toLowerCase())) {
                            try {
                                imageUtils.wrapDeviceFrames(file.toString(),
                                    format("{0}{1}{2}/{3}/{4}/failed_{5}.jpeg",
                                        getProperty("user.dir"),
                                        FileLocations.SCREENSHOTS_DIRECTORY, device,
                                        AppiumDeviceManager.getAppiumDevice()
                                            .getDevice()
                                            .getUdid()
                                            .replaceAll("\\W", "_"), deviceModel,
                                        failed_StepName.replaceAll(" ", "_")),
                                    format("{0}{1}{2}/{3}/{4}/failed_{5}_framed.jpeg",
                                        getProperty("user.dir"),
                                        FileLocations.SCREENSHOTS_DIRECTORY, device,
                                        AppiumDeviceManager.getAppiumDevice()
                                            .getDevice()
                                            .getUdid()
                                            .replaceAll("\\W", "_"), deviceModel,
                                        failed_StepName.replaceAll(" ", "_")));
                                break;
                            } catch (InterruptedException | IM4JavaException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Resource Directory was not found");
        }
    }

    public void attachScreenShotToReport(String stepName) throws IOException {
        String platform = null;
        if (AppiumDeviceManager.getMobilePlatform()
            .equals(MobilePlatform.ANDROID)) {
            platform = "android";
        } else if (AppiumDeviceManager.getMobilePlatform()
            .equals(MobilePlatform.IOS)) {
            platform = "iPhone";
        }
        File framedImageAndroid = new File(
            format("{0}{1}{2}/{3}/{4}/failed_{5}_framed.jpeg", getProperty("user.dir"),
                FileLocations.SCREENSHOTS_DIRECTORY, platform, AppiumDeviceManager.getAppiumDevice()
                    .getDevice()
                    .getUdid(), deviceModel, stepName.replaceAll(" ", "_")));
    }

    @Override
    public void onStart(ISuite iSuite) {
        try {
            appiumServerManager.startAppiumServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFinish(ISuite iSuite) {
        try {
            appiumServerManager.stopAppiumServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}