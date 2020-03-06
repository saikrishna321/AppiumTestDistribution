package com.appium.executor;

import java.io.IOException;

import com.github.ios.IOSDeviceConfiguration;
import com.github.utils.CommandPrompt;

/**
 * Created by saikrisv on 30/05/17.
 */
public class IOSDeviceTest {

    IOSDeviceConfiguration iosDeviceConfiguration;
    CommandPrompt          cmd;

    public IOSDeviceTest() throws IOException {
        iosDeviceConfiguration = new IOSDeviceConfiguration();
        cmd = new CommandPrompt();
    }
}
