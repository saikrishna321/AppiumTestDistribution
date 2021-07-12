package com.appium.manager;

import com.appium.device.DevicesByHost;
import com.appium.device.HostMachineDeviceManager;
import com.appium.utils.ArtifactsUploader;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * DeviceAllocationManager - Handles device initialisation, allocation and de-allocattion
 */
public class DeviceAllocationManager extends ArtifactsUploader {

    private static DeviceAllocationManager instance;
    private static final Logger LOGGER = Logger.getLogger(DeviceAllocationManager.class.getName());
    private HostMachineDeviceManager hostMachineDeviceManager;
    private List<AppiumDevice> allDevices;
    private List<Thread> suspendedThreads;

    private DeviceAllocationManager() throws Exception {
        suspendedThreads = new ArrayList<>();
        hostMachineDeviceManager = HostMachineDeviceManager.getInstance();
        ArtifactsUploader.getInstance().initializeArtifacts();
        DevicesByHost appiumDeviceByHost = hostMachineDeviceManager.getDevicesByHost();
        allDevices = appiumDeviceByHost.getAllDevices();

    }


    public static DeviceAllocationManager getInstance()  {
        if (instance == null) {
            try {
                instance = new DeviceAllocationManager();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }


    public List<AppiumDevice> getDevices() {
        LOGGER.info("All devices connected");
        return hostMachineDeviceManager.getDevicesByHost().getAllDevices();
    }

    public synchronized AppiumDevice getNextAvailableDevice() {
        int i = 0;
        for (AppiumDevice device : allDevices) {
            Thread t = Thread.currentThread();
            t.setName("Thread_" + i);
            i++;
            if (device.isAvailable()) {
                device.blockDevice();
                return device;
            }
        }
        if (Thread.activeCount() > allDevices.size()) {
            suspendedThreads.add(Thread.currentThread());
            Thread.currentThread().suspend();
        }
        return null;
    }

    public synchronized void freeDevice() {
        AppiumDeviceManager.getAppiumDevice().freeDevice();
        LOGGER.info("DeAllocated Device " + AppiumDeviceManager.getAppiumDevice().getDevice()
                .getUdid()
                + " from execution list");
        if (suspendedThreads.size() > 0) {
            suspendedThreads.get(0).resume();
            suspendedThreads.remove(0);
        }
    }

    public void allocateDevice(AppiumDevice appiumDevice) {
        LOGGER.info("Allocated Device for execution: " + appiumDevice);
        AppiumDeviceManager.setDevice(appiumDevice);
    }

}
