package com.appium.device;

import static com.appium.utils.OverriddenVariable.getOverriddenStringValue;

public class AtdEnvironment {
    public String get(String environmentVariableName) {
        return getOverriddenStringValue(environmentVariableName);
    }
}
