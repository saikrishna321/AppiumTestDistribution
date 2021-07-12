package com.appium.manager;

import com.appium.capabilities.DesiredCapabilityBuilder;
import com.appium.entities.MobilePlatform;
import com.appium.utils.CommandPrompt;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.windows.WindowsDriver;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;

import static com.appium.manager.AppiumDeviceManager.getMobilePlatform;
import static com.appium.utils.ConfigFileManager.CAPS;

public class AppiumDriverManager {
    private static ThreadLocal<AppiumDriver> appiumDriver = new ThreadLocal<>();
    private DesiredCapabilityBuilder desiredCapabilityBuilder;
    private static final Logger LOGGER = Logger.getLogger(AppiumDriverManager.class.getName());

    public AppiumDriverManager() {
        desiredCapabilityBuilder = new DesiredCapabilityBuilder();
    }

    public static AppiumDriver getDriver() {
        return appiumDriver.get();
    }

    protected static void setDriver(AppiumDriver driver) {
        LOGGER.info("AppiumDriverManager: Created AppiumDriver with capabilities: ");
        Capabilities capabilities = driver.getCapabilities();
        capabilities.getCapabilityNames().forEach(key -> {
            LOGGER.info("\t" + key + ":: " + capabilities.getCapability(key));
        });
        appiumDriver.set(driver);
    }


    private AppiumDriver<MobileElement> initialiseDriver(DesiredCapabilities desiredCapabilities)
            throws Exception {
        LOGGER.info("Initialise Driver with Capabilities: " + desiredCapabilities.toString());
        String remoteWDHubIP = getRemoteWDHubIP();
        return createAppiumDriver(desiredCapabilities, remoteWDHubIP);
    }

    @NotNull
    private AppiumDriver createAppiumDriver(DesiredCapabilities desiredCapabilities,
                                             String remoteWDHubIP)
            throws MalformedURLException {
        AppiumDriver currentDriverSession;
        MobilePlatform mobilePlatform = getMobilePlatform();
        switch (mobilePlatform) {
            case IOS:
                currentDriverSession = new IOSDriver(new URL(remoteWDHubIP),
                        desiredCapabilities);
                break;
            case ANDROID:
                currentDriverSession = new AndroidDriver(new URL(remoteWDHubIP),
                        desiredCapabilities);
                break;
            case WINDOWS:
                currentDriverSession = new WindowsDriver(new URL(remoteWDHubIP),
                        desiredCapabilities);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mobilePlatform);
        }
        LOGGER.info("Session Created for "
                + AppiumDeviceManager.getMobilePlatform().name()
                + " ---- "
                + currentDriverSession.getSessionId() + "---"
                + currentDriverSession.getSessionDetail("udid"));
        return currentDriverSession;
    }

    private String getRemoteWDHubIP() throws Exception {
        String hostName = AppiumDeviceManager.getAppiumDevice().getHostName();
        IAppiumManager appiumManager = AppiumManagerFactory.getAppiumManager(hostName);
        return appiumManager.getRemoteWDHubIP(hostName);
    }


    private AppiumDriver<MobileElement> startAppiumDriverInstance(
            Optional<DesiredCapabilities> desiredCapabilities)
        throws Exception {
        LOGGER.info("startAppiumDriverInstance: capabilities: " + desiredCapabilities);
        AppiumDriver<MobileElement> currentDriverSession =
                initialiseDriver(desiredCapabilities.get());
        AppiumDriverManager.setDriver(currentDriverSession);
        return currentDriverSession;
    }

    // Should be used by Cucumber as well
    public AppiumDriver<MobileElement> startAppiumDriverInstance() throws Exception {
        LOGGER.info("startAppiumDriverInstance");
        return startAppiumDriverInstance(Optional.ofNullable(buildDesiredCapabilities(CAPS.get())));
    }

    private DesiredCapabilities buildDesiredCapabilities(String capabilityFilePath)
        throws Exception {
        String absolutePathToCapabilities = capabilityFilePath;
        if (new File(capabilityFilePath).exists()) {
            Path path = FileSystems.getDefault().getPath(capabilityFilePath);
            if (!path.getParent().isAbsolute()) {
                absolutePathToCapabilities = path.normalize()
                    .toAbsolutePath().toString();
            }
            desiredCapabilityBuilder
                .buildDesiredCapability(absolutePathToCapabilities);
            return DesiredCapabilityBuilder.getDesiredCapability();
        } else {
            throw new RuntimeException("Capability file not found");
        }
    }

    public void stopAppiumDriver() throws Exception {
        String OS = System.getProperty("os.name").toLowerCase();
        String command;
        AppiumDevice appiumDevice = AppiumDeviceManager.getAppiumDevice();
        if (appiumDevice.getDevice().getOs().equalsIgnoreCase("iOS")
            && appiumDevice.getDevice().isDevice()) {
            String hostName = appiumDevice.getHostName();
            AppiumManagerFactory.getAppiumManager(hostName).destoryIOSWebKitProxy(hostName);
        }
        if (appiumDevice.getChromeDriverPort() > 0) {
            if (OS.contains("mac")) {
                command = "kill -9 $(lsof -ti tcp:"
                    + appiumDevice.getChromeDriverPort() + ")";
                new CommandPrompt().runCommand(command);
            }
            appiumDevice.setChromeDriverPort(0);
        }
        if (AppiumDriverManager.getDriver() != null
            && AppiumDriverManager.getDriver().getSessionId() != null) {
            LOGGER.info("Session Deleting ---- "
                + AppiumDriverManager.getDriver().getSessionId() + "---"
                + AppiumDriverManager.getDriver().getSessionDetail("udid"));
            AppiumDriverManager.getDriver().quit();
        }
    }
}