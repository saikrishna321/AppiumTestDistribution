package com.appium.capabilities;

import com.appium.entities.MobilePlatform;
import com.appium.filelocations.FileLocations;
import com.appium.ios.IOSDeviceConfiguration;
import com.appium.manager.AppiumDevice;
import com.appium.manager.AppiumDeviceManager;
import com.appium.utils.ArtifactsUploader;
import com.appium.utils.AvailablePorts;
import com.appium.utils.HostArtifact;
import com.appium.utils.JsonParser;
import com.github.device.Device;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import static com.appium.manager.AppiumDeviceManager.isPlatform;
import static com.appium.utils.OverriddenVariable.getOverriddenStringValue;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Created by saikrisv on 20/05/17.
 */
public class DesiredCapabilityBuilder extends ArtifactsUploader {

    private static final Logger LOGGER = Logger.getLogger(DesiredCapabilityBuilder.class.getName());
    public static final String APP_PACKAGE = "APP_PACKAGE";
    private AvailablePorts availablePorts;

    public static ThreadLocal<DesiredCapabilities> desiredCapabilitiesThreadLocal
        = new ThreadLocal<>();

    public DesiredCapabilityBuilder() {
        super();
        availablePorts = new AvailablePorts();
    }

    public static DesiredCapabilities getDesiredCapability() {
        return desiredCapabilitiesThreadLocal.get();
    }

    public void buildDesiredCapability(String jsonPath) throws Exception {
        int port = AppiumDeviceManager.getAppiumDevice().getPort();
        String platform = AppiumDeviceManager.getAppiumDevice().getDevice().getOs();
        boolean isCloud = AppiumDeviceManager.getAppiumDevice().getDevice().isCloud();
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        if (isCloud) {
            desiredCapabilityForCloud(platform, jsonPath, desiredCapabilities);
        } else {
            desiredCapabilityForLocalAndRemoteATD(platform, jsonPath, port, desiredCapabilities);

        }
    }

    private void desiredCapabilityForCloud(String platform, String jsonPath,
                                           DesiredCapabilities desiredCapabilities) {
        JSONObject platFormCapabilities = new JsonParser(jsonPath).getObjectFromJSON()
            .getJSONObject(platform);
        platFormCapabilities.keySet().forEach(key -> {
            capabilityObject(desiredCapabilities, platFormCapabilities, key);
        });
        AppiumDevice deviceProperty = AppiumDeviceManager.getAppiumDevice();
        desiredCapabilities.setCapability("device",
            deviceProperty.getDevice().getName());
        desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME,
            deviceProperty.getDevice().getName());
        Object pCloudyApiKey = desiredCapabilities.getCapability("pCloudy_ApiKey");
        if (null == pCloudyApiKey) {
            desiredCapabilities.setCapability(CapabilityType.BROWSER_NAME, "");
            desiredCapabilities.setCapability(CapabilityType.VERSION,
                    deviceProperty.getDevice().getOsVersion());
            desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION,
                    deviceProperty.getDevice().getOsVersion());
        }
        LOGGER.info("desiredCapabilityForCloud: " + desiredCapabilities);
        desiredCapabilitiesThreadLocal.set(desiredCapabilities);
    }

    private void capabilityObject(DesiredCapabilities desiredCapabilities,
                                  JSONObject platFormCapabilities, String key) {
        String appCapability = "app";
        if (appCapability.equals(key)) {
            Object values = platFormCapabilities.get(appCapability);
            List<HostArtifact> hostArtifacts = ArtifactsUploader.getInstance()
                .getHostArtifacts();
            String hostAppPath = hostAppPath(values, hostArtifacts);
            Path path = FileSystems.getDefault().getPath(hostAppPath);
            if (AppiumDeviceManager.getAppiumDevice().getDevice().isCloud()
                || new UrlValidator().isValid(hostAppPath)) {
                desiredCapabilities.setCapability(appCapability, hostAppPath);
            } else {
                desiredCapabilities.setCapability(appCapability,
                    path.normalize().toAbsolutePath().toString());
            }
        } else {
            desiredCapabilities.setCapability(key, platFormCapabilities.get(key));
        }
    }

    private void desiredCapabilityForLocalAndRemoteATD(String platform,
                                                       String jsonPath,
                                                       int port,
                                                       DesiredCapabilities desiredCapabilities)
        throws Exception {
        JSONObject platFormCapabilities = new JsonParser(jsonPath).getObjectFromJSON()
            .getJSONObject(platform);

        platFormCapabilities.keySet().forEach(key -> {
            capabilityObject(desiredCapabilities, platFormCapabilities, key);
        });

        if (isPlatform(MobilePlatform.ANDROID)) {
            try {
                AppiumDeviceManager.getAppiumDevice().setChromeDriverPort(
                        availablePorts.getAvailablePort(AppiumDeviceManager
                                .getAppiumDevice().getHostName()));
                desiredCapabilities.setCapability(AndroidMobileCapabilityType
                    .CHROMEDRIVER_PORT,
                        AppiumDeviceManager.getAppiumDevice().getChromeDriverPort());
                desiredCapabilities.setCapability(AndroidMobileCapabilityType
                            .CHROMEDRIVER_EXECUTABLE,
                    AppiumDeviceManager.getAppiumDevice().getChromeDriverExecutable());
            } catch (Exception e) {
                throw new RuntimeException("Unable to allocate chromedriver with unique port");
            }
            desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "android");
            desiredCapabilities.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT,
                port);
            appPackage(desiredCapabilities);
        } else if (AppiumDeviceManager.getMobilePlatform().equals(MobilePlatform.IOS)) {
            appPackageBundle(desiredCapabilities);
            //Check if simulator.json exists and add the deviceName and OS
            if (AppiumDeviceManager.getAppiumDevice().getDevice().getUdid().length()
                == IOSDeviceConfiguration.SIM_UDID_LENGTH) {

                AppiumDevice deviceProperty = AppiumDeviceManager.getAppiumDevice();
                desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME,
                    deviceProperty.getDevice().getName());
                desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION,
                    deviceProperty.getDevice().getOsVersion());
                desiredCapabilities.setCapability("webkitDebugProxyPort",
                    new AvailablePorts().getAvailablePort(AppiumDeviceManager
                        .getAppiumDevice().getHostName()));
            } else {
                desiredCapabilities.setCapability("webkitDebugProxyPort",
                    new AvailablePorts().getAvailablePort(AppiumDeviceManager
                        .getAppiumDevice().getHostName()));
            }

            desiredCapabilities.setCapability(IOSMobileCapabilityType
                .WDA_LOCAL_PORT, port);
            desiredCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME,
                "XCUITest");
            desiredCapabilities.setCapability(MobileCapabilityType.UDID,
                AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
            File derivedDataPath = new File(System.getProperty("user.dir")
                + FileLocations.DERIVED_DATA
                + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
            if (!derivedDataPath.exists()) {
                derivedDataPath.mkdirs();
            }
            desiredCapabilities.setCapability("derivedDataPath", derivedDataPath.getAbsolutePath());
        }
        desiredCapabilities.setCapability(MobileCapabilityType.UDID,
            AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
        desiredCapabilitiesThreadLocal.set(desiredCapabilities);
    }

    private String hostAppPath(Object values, List<HostArtifact> hostArtifacts) {
        HostArtifact hostArtifact;
        hostArtifact = hostArtifacts.stream().filter(s ->
            s.getHost()
                .equalsIgnoreCase(AppiumDeviceManager
                    .getAppiumDevice()
                    .getHostName()))
            .collect(toList()).parallelStream()
            .findFirst().get();
        String appPath = hostArtifact.getArtifactPath("APK");
        if (values instanceof JSONObject) {
            Device device = AppiumDeviceManager.getAppiumDevice()
                .getDevice();
            if (!device.isCloud()) {
                String deviceOS = device
                    .getOs();
                if (deviceOS.equalsIgnoreCase("iOS") && !device.isDevice()) {
                    appPath = hostArtifact.getArtifactPath("APP");
                } else if (deviceOS.equals("iOS") && device.isDevice()) {
                    appPath = hostArtifact.getArtifactPath("IPA");
                } else if (deviceOS.equals("windows") && device.isDevice()) {
                    appPath = hostArtifact.getArtifactPath("EXE");
                }
            } else {
                if (isEmpty(appPath) || (((JSONObject) values).has("simulator")
                    || ((JSONObject) values).has("device"))) {
                    appPath = hostArtifact.getArtifactPath(((JSONObject) values)
                        .has("simulator")
                        ? "APP" : "IPA");
                }
            }
        }
        return appPath;
    }

    private void appPackage(DesiredCapabilities desiredCapabilities) {
        if (getOverriddenStringValue(APP_PACKAGE) != null) {
            desiredCapabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE,
                getOverriddenStringValue(APP_PACKAGE));
        }
    }

    private void appPackageBundle(DesiredCapabilities iOSCapabilities) {
        if (getOverriddenStringValue(APP_PACKAGE) != null) {
            iOSCapabilities
                .setCapability(IOSMobileCapabilityType.BUNDLE_ID,
                    getOverriddenStringValue(APP_PACKAGE));
        }
    }
}
