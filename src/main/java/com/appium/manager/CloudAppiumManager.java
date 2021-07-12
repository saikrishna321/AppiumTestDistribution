package com.appium.manager;

import com.github.device.Device;
import org.apache.log4j.Logger;

import java.util.List;

import static java.lang.System.getenv;
import static java.text.MessageFormat.format;

public class CloudAppiumManager implements IAppiumManager {
    private final String url;
    private static final Logger LOGGER = Logger.getLogger(CloudAppiumManager.class.getName());

    public CloudAppiumManager(String cloudName) {
        url = "https://{2}/wd/hub";
    }

    public CloudAppiumManager() {
        url = "https://{0}:{1}@{2}/wd/hub";
    }

    @Override
    public void destroyAppiumNode(String host) {

    }

    @Override
    public String getRemoteWDHubIP(String host) {
        String format = format(url, getenv("CLOUD_USER"), getenv("CLOUD_KEY"), host);
        LOGGER.info("getRemoteWDHubIP: " + format);
        return format;
    }

    @Override
    public void startAppiumServer(String host) {

    }

    @Override
    public List<Device> getDevices(String machineIP, String platform) {
        return null;
    }

    @Override
    public Device getSimulator(String machineIP, String deviceName, String os) {
        return null;
    }

    @Override
    public int getAvailablePort(String hostMachine) {
        return 0;
    }

    @Override
    public int startIOSWebKitProxy(String host) {
        return 0;
    }

    @Override
    public void destoryIOSWebKitProxy(String host) {

    }
}
