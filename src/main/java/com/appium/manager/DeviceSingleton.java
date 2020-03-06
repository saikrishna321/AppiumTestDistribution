package com.appium.manager;

import com.github.manager.DeviceAllocationManager;

/**
 * Created by saikrisv on 18/05/17.
 */
public class DeviceSingleton {

    private static DeviceSingleton         instance;
    private static DeviceAllocationManager deviceID = null;

    public static DeviceSingleton getInstance() throws Exception {
        if (instance == null) {
            instance = new DeviceSingleton();
            deviceID = DeviceAllocationManager.getInstance();
        }
        return instance;
    }
}
