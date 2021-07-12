package com.appium.manager;

import com.appium.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;

public class AppiumManagerFactory {
    public IAppiumManager getAppiumManagerFor(String host) {
        return AppiumManagerFactory.getAppiumManager(host);
    }

    public static IAppiumManager getAppiumManager(String host) {
        Capabilities capabilities = Capabilities.getInstance();
        if (capabilities.isCloud(host)) {
            String cloudName = capabilities.getCloudName(host);
            return createCloudAppiumManagerForCloud(cloudName);
        } else if ("127.0.0.1".equals(host)) {
            return new LocalAppiumManager();
        } else {
            return new RemoteAppiumManager();
        }
    }

    @NotNull
    private static CloudAppiumManager createCloudAppiumManagerForCloud(String cloudName) {
        if (cloudName.equalsIgnoreCase("pCloudy")) {
            return new CloudAppiumManager(cloudName);
        } else {
            return new CloudAppiumManager();
        }
    }
}

