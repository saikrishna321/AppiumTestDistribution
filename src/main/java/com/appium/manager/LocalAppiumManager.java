package com.appium.manager;

import com.appium.exceptions.LocalConnectionException;
import com.appium.filelocations.FileLocations;
import com.appium.capabilities.Capabilities;
import com.appium.utils.OSType;
import com.github.android.AndroidManager;
import com.github.device.Device;
import com.github.device.SimulatorManager;
import com.github.iOS.IOSManager;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LocalAppiumManager implements IAppiumManager {

    private static AppiumDriverLocalService appiumDriverLocalService;

    private static AppiumDriverLocalService getAppiumDriverLocalService() {
        return appiumDriverLocalService;
    }

    private static final Logger LOGGER = Logger.getLogger(LocalAppiumManager.class.getName());

    private static void setAppiumDriverLocalService(
            AppiumDriverLocalService appiumDriverLocalService) {
        LocalAppiumManager.appiumDriverLocalService = appiumDriverLocalService;
    }

    private URL getAppiumUrl() {
        return getAppiumDriverLocalService().getUrl();
    }


    @Override
    public void destroyAppiumNode(String host) {
        getAppiumDriverLocalService().stop();
        if (getAppiumDriverLocalService().isRunning()) {
            LOGGER.info("AppiumServer didn't shut... Trying to quit again....");
            getAppiumDriverLocalService().stop();
        }
    }

    @Override
    public String getRemoteWDHubIP(String host) {
        return getAppiumUrl().toString();
    }

    @Override
    public void startAppiumServer(String host) throws Exception {
        LOGGER.info(LOGGER.getName() + "Starting Appium Server on Localhost");
        AppiumDriverLocalService appiumDriverLocalService;
        AppiumServiceBuilder builder =
                getAppiumServerBuilder(host)
                        .withLogFile(new File(
                                System.getProperty("user.dir")
                                        + FileLocations.APPIUM_LOGS_DIRECTORY
                                        + "appium_logs.txt"))
                        .withIPAddress(host)
                        .withArgument(GeneralServerFlag.RELAXED_SECURITY)
                        .usingAnyFreePort();
        appiumDriverLocalService = builder.build();
        appiumDriverLocalService.start();
        LOGGER.info(LOGGER.getName() + "Appium Server Started at......"
                + appiumDriverLocalService.getUrl());
        setAppiumDriverLocalService(appiumDriverLocalService);
    }

    @Override
    public List<Device> getDevices(String machineIP, String platform) {
        List<Device> devices = new ArrayList<>();
        getAndroidDevices(platform, devices);
        getIOSDevices(platform, devices);
        getWindowsDevice(platform, devices);
        return devices;
    }

    private void getWindowsDevice(String platform, List<Device> devices) {
        if (platform.equalsIgnoreCase(OSType.WINDOWS.name())
                && Capabilities.getInstance().isWindowsApp()) {
            Device device = new Device();
            device.setName("windows");
            device.setOs("windows");
            device.setName("windows");
            device.setUdid("win-123");
            device.setDevice(true);
            List<Device> deviceList = new ArrayList<>();
            deviceList.add(device);
            devices.addAll(deviceList);
        }
    }

    private void getIOSDevices(String platform, List<Device> devices) {
        if (platform.equalsIgnoreCase(OSType.iOS.name())
                || platform.equalsIgnoreCase(OSType.BOTH.name())) {
            if (Capabilities.getInstance().isApp()) {
                if (Capabilities.getInstance().isSimulatorAppPresentInCapsJson()) {
                    try {
                        devices.addAll(new SimulatorManager()
                                .getAllBootedSimulators(OSType.iOS.name()));
                    } catch (InterruptedException | IOException e) {
                        throw new LocalConnectionException(
                                "Exception getting booted simulators", e);
                    }
                }
                if (Capabilities.getInstance().isRealDeviceAppPresentInCapsJson()) {
                    devices.addAll(new IOSManager().getDevices());
                }
            } else {
                devices.addAll(new IOSManager().getDevices());
            }
        }
    }

    private void getAndroidDevices(String platform, List<Device> devices) {
        if (platform.equalsIgnoreCase(OSType.ANDROID.name())
                || platform.equalsIgnoreCase(OSType.BOTH.name())) {
            try {
                devices.addAll(new AndroidManager().getDevices());
            } catch (Exception e) {
                throw new LocalConnectionException("Exception getting devices", e);
            }
        }
    }

    @Override
    public Device getSimulator(String machineIP, String deviceName, String os) {
        try {
            return new SimulatorManager().getDevice(deviceName, os, OSType.iOS.name());
        } catch (InterruptedException | IOException e) {
            throw new LocalConnectionException("Exception getting simulators", e);
        }
    }

    @Override
    public int getAvailablePort(String hostMachine) throws IOException {
        ServerSocket socket = new ServerSocket(0);
        socket.setReuseAddress(true);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    @Override
    public int startIOSWebKitProxy(String host) throws Exception {
        int port = getAvailablePort(host);
        String webkitRunner = "ios_webkit_debug_proxy -c "
                + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid()
                + ":" + port;
        String process = Runtime.getRuntime().exec(webkitRunner).toString();
        AppiumDeviceManager.getAppiumDevice().setWebkitProcessID(process);
        return port;
    }

    @Override
    public void destoryIOSWebKitProxy(String host) throws Exception {
        if (AppiumDeviceManager.getAppiumDevice().getWebkitProcessID() != null) {
            String command = "kill -9 " + AppiumDeviceManager
                    .getAppiumDevice().getWebkitProcessID();
            LOGGER.info("Kills webkit proxy" + "******************" + command);
            Runtime.getRuntime().exec(command);
            AppiumDeviceManager.getAppiumDevice().setWebkitProcessID(null);
        }
    }

    private AppiumServiceBuilder getAppiumServerBuilder(String host) throws Exception {
        if (Capabilities.getInstance().getAppiumServerPath(host) == null) {
            LOGGER.info("Picking Default Path for AppiumServiceBuilder");
            return getAppiumServiceBuilderWithDefaultPath();
        } else {
            LOGGER.info("Picking UserSpecified Path for AppiumServiceBuilder");
            return getAppiumServiceBuilderWithUserAppiumPath(host);
        }
    }

    private AppiumServiceBuilder getAppiumServiceBuilderWithUserAppiumPath(String host)
            throws Exception {
        return new AppiumServiceBuilder().withAppiumJS(
                new File(Capabilities.getInstance().getAppiumServerPath(host)));
    }

    private AppiumServiceBuilder getAppiumServiceBuilderWithDefaultPath() {
        return new AppiumServiceBuilder();
    }

}
