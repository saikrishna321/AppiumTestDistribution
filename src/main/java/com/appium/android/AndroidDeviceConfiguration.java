package com.appium.android;

import com.appium.ios.IOSDeviceConfiguration;
import com.appium.manager.AppiumDeviceManager;

import com.appium.manager.AppiumDevice;
import com.appium.utils.CommandPrompt;
import com.appium.device.DevicesByHost;
import com.appium.device.HostMachineDeviceManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AndroidDeviceConfiguration {

    private CommandPrompt cmd = new CommandPrompt();
    public static List<String> validDeviceIds = new ArrayList<>();
    private DevicesByHost devicesByHost;
    private static final Logger LOGGER = Logger.getLogger(
            AndroidDeviceConfiguration.class.getName());

    public AndroidDeviceConfiguration() {
        devicesByHost = HostMachineDeviceManager.getInstance().getDevicesByHost();
    }

    /*
     * This method gets the device model name
     */
    public String getDeviceModel() {
        AppiumDevice getModel = getDevice();
        return (getModel.getDevice().getDeviceModel()
                + getModel.getDevice().getBrand())
                .replaceAll("[^a-zA-Z0-9.\\-]", "");
    }

    /*
     * This method gets the device OS API Level
     */
    public String getDeviceOS() {
        AppiumDevice deviceOS = getDevice();
        return deviceOS.getDevice().getOsVersion();
    }

    private AppiumDevice getDevice() {
        return AppiumDeviceManager.getAppiumDevice();
    }

    public String screenRecord(String fileName) {
        return "adb -s " + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid()
                + " shell screenrecord --bit-rate 3000000 /sdcard/" + fileName
                + ".mp4";
    }

    public boolean checkIfRecordable() throws IOException, InterruptedException {
        String screenrecord =
                cmd.runCommand("adb -s " + AppiumDeviceManager
                        .getAppiumDevice().getDevice().getUdid()
                        + " shell ls /system/bin/screenrecord");
        return screenrecord.trim().equals("/system/bin/screenrecord");
    }

    public String getDeviceManufacturer() {
        return devicesByHost.getDeviceProperty(AppiumDeviceManager
                .getAppiumDevice().getDevice().getUdid())
                .getDevice().getDeviceManufacturer();
    }

    public AndroidDeviceConfiguration pullVideoFromDevice(String fileName, String destination)
            throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder("adb", "-s",
                        AppiumDeviceManager.getAppiumDevice().getDevice().getUdid(),
                        "pull", "/sdcard/" + fileName + ".mp4",
                        destination);
        Process pc = pb.start();
        pc.waitFor();
        LOGGER.info("Exited with Code::" + pc.exitValue());
        Thread.sleep(5000);
        return new AndroidDeviceConfiguration();
    }

    public void removeVideoFileFromDevice(String fileName)
            throws IOException, InterruptedException {
        cmd.runCommand("adb -s " + AppiumDeviceManager
                .getAppiumDevice().getDevice().getUdid()
                + " shell rm -f /sdcard/"
                + fileName + ".mp4");
    }

    public void setValidDevices(List<String> deviceID) {
        deviceID.forEach(deviceList -> {
            if (deviceList.length() < IOSDeviceConfiguration.IOS_UDID_LENGTH) {
                validDeviceIds.add(deviceList);
            }
        });
    }
}
