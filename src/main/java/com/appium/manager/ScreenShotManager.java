package com.appium.manager;

import com.appium.android.AndroidDeviceConfiguration;
import com.appium.entities.MobilePlatform;
import com.appium.filelocations.FileLocations;
import com.appium.manager.AppiumDeviceManager;
import com.appium.manager.AppiumDriverManager;
import com.appium.manager.AppiumParallelMethodTestListener;
import com.appium.utils.Helpers;
import com.appium.utils.ImageUtils;
import com.epam.reportportal.service.ReportPortal;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.im4java.core.IM4JavaException;
import org.openqa.selenium.OutputType;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

import static com.appium.manager.AppiumDeviceManager.getMobilePlatform;

/**
 * Created by saikrisv on 26/04/17.
 */
public class ScreenShotManager extends Helpers {
    private static final Logger LOGGER = Logger.getLogger(ScreenShotManager.class.getName());
    private String screenShotNameWithTimeStamp;
    private ImageUtils imageUtils;
    private String capturedScreen;
    private String failedScreen;
    private String framedFailedScreen;
    private String framedCapturedScreen;

    private String getFramedCapturedScreen() {
        return framedCapturedScreen;
    }

    private void setFramedCapturedScreen(String framedCapturedScreen) {
        this.framedCapturedScreen = framedCapturedScreen;
    }

    protected String getCapturedScreen() {
        return capturedScreen;
    }

    private void setCapturedScreen(String capturedScreen) {
        this.capturedScreen = capturedScreen;
    }

    public String getFramedFailedScreen() {
        return framedFailedScreen;
    }

    private void setFramedFailedScreen(String framedFailedScreen) {
        this.framedFailedScreen = framedFailedScreen;
    }

    public String getFailedScreen() {
        return failedScreen;
    }

    private void setFailedScreen(String failedScreen) {
        this.failedScreen = failedScreen;
    }


    public ScreenShotManager() {
        imageUtils = new ImageUtils();
    }

    public String captureScreenShot(int status, String className, String screenShotName,
                                    String methodName, String deviceModel) {
        String getDeviceModel = null;
        if (AppiumDriverManager.getDriver().getSessionId() != null) {
            LOGGER.info("Current Running Thread Status"
                    + AppiumDriverManager.getDriver().getSessionId());
            File scrFile = AppiumDriverManager.getDriver()
                    .getScreenshotAs(OutputType.FILE);
            screenShotNameWithTimeStamp = currentDateAndTime();
            if (getMobilePlatform().equals(MobilePlatform.ANDROID)) {
                getDeviceModel = screenShotNameWithTimeStamp;
                screenShotAndFrame(status, scrFile, methodName, className, getDeviceModel,
                        "android", deviceModel, screenShotName);
            } else if (getMobilePlatform().equals(MobilePlatform.IOS)) {
                getDeviceModel = screenShotNameWithTimeStamp;
                screenShotAndFrame(status, scrFile, methodName, className, getDeviceModel,
                        "iOS", deviceModel, screenShotName);
            }
        }
        return getDeviceModel;
    }

    public void captureScreenShot(String screenShotName) {
        String className = getCurrentTestClassName();
        String methodName = getCurrentTestMethodName();
        String deviceModel = null;
        MobilePlatform mobilePlatform = getMobilePlatform();
        switch (mobilePlatform) {
            case IOS:
                deviceModel = AppiumDeviceManager.getAppiumDevice()
                                      .getDevice().getDeviceModel();
                break;
            case ANDROID:
                deviceModel = new AndroidDeviceConfiguration().getDeviceModel();
                break;
            case WINDOWS:
                deviceModel = AppiumDeviceManager
                        .getAppiumDevice()
                        .getDevice()
                        .getDeviceModel();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mobilePlatform);
        }
        captureScreenShot(1, className, screenShotName, methodName, deviceModel);
    }


    private String currentDateAndTime() {
        LocalDateTime rightNow = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
                FormatStyle.MEDIUM).withLocale(Locale.getDefault());
        return dateTimeFormatter.format(rightNow).replaceAll("[- .:,]", "_");
    }

    private void screenShotAndFrame(int status,
                                    File scrFile, String methodName,
                                    String className, String model,
                                    String platform, String deviceModel,
                                    String screenShotName) {
        String udid = AppiumDeviceManager.getAppiumDevice().getDevice().getUdid();
        setFailedScreen(
                "screenshot/" + platform + "/" + udid
                        + "/" + className + "/"
                        + methodName + "/"
                        + screenShotNameWithTimeStamp + "-"
                        + screenShotName + "_failed" + ".jpeg");
        setCapturedScreen(
                "screenshot/" + platform + "/" + udid
                        + "/" + className
                        + "/" + methodName + "/"
                        + screenShotNameWithTimeStamp + "-"
                        + screenShotName + "_results.jpeg");

        setFramedCapturedScreen("screenshot/" + platform + "/" + udid
                + "/" + className
                + "/" + methodName + "/"
                + screenShotNameWithTimeStamp + "-"
                + screenShotName + "_results.jpeg");
        setFramedFailedScreen(
                "screenshot/" + platform + "/" + udid
                        + "/" + className + "/" + methodName + "/"
                        + screenShotNameWithTimeStamp + "-"
                        + screenShotName + "_failed.jpeg");

        try {
            File framePath =
                    new File(System.getProperty("user.dir")
                            + "/src/test/resources/frames/");
            if (status == ITestResult.FAILURE) {
                FileUtils.copyFile(scrFile, new File(System.getProperty("user.dir")
                        + FileLocations.OUTPUT_DIRECTORY + getFailedScreen().trim()));
            } else {
                String capturedScreenshotPath = System.getProperty("user.dir")
                        + FileLocations.OUTPUT_DIRECTORY + getCapturedScreen().trim();
                FileUtils.copyFile(scrFile, new File(capturedScreenshotPath));
            }

            File[] files1 = framePath.listFiles();
            if (framePath.exists()) {
                for (int i = 0; i < files1.length; i++) {
                    if (files1[i].isFile()) {
                        Path p = Paths.get(files1[i].toString());
                        String fileName = p.getFileName().toString().toLowerCase();
                        if (deviceModel.toLowerCase()
                                .contains(fileName.split(".png")[0].toLowerCase())) {
                            try {
                                if (status == ITestResult.FAILURE) {
                                    String screenToFrame = System.getProperty("user.dir")
                                            + FileLocations.OUTPUT_DIRECTORY + getFailedScreen();
                                    imageUtils.wrapDeviceFrames(files1[i].toString(), screenToFrame,
                                            System.getProperty("user.dir")
                                                    + FileLocations.OUTPUT_DIRECTORY
                                                    + getFramedFailedScreen());
                                } else {
                                    String screenToFrame = System.getProperty("user.dir")
                                            + FileLocations.OUTPUT_DIRECTORY + getCapturedScreen();
                                    imageUtils.wrapDeviceFrames(files1[i].toString(), screenToFrame,
                                            System.getProperty("user.dir")
                                                    + FileLocations.OUTPUT_DIRECTORY
                                                    + getFramedCapturedScreen());
                                }

                                break;
                            } catch (InterruptedException | IM4JavaException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
