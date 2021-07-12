package com.appium.utils;

public class OverriddenVariable {
    public static String getOverriddenStringValue(String key) {
        return getValueFromEnvOrProperty(key);
    }

    public static String getOverriddenStringValue(String key, String defaultValue) {
        return (isKeyProvidedInEnvOrProperty(key))
                ? getValueFromEnvOrProperty(key)
                : defaultValue;
    }

    public static int getOverriddenIntValue(String key) {
        return Integer.parseInt(getValueFromEnvOrProperty(key));
    }

    public static int getOverriddenIntValue(String key, int defaultValue) {
        return isKeyProvidedInEnvOrProperty(key)
                ? Integer.parseInt(getValueFromEnvOrProperty(key))
                : defaultValue;
    }

    public static boolean getOverriddenBooleanValue(String key) {
        return Boolean.parseBoolean(getValueFromEnvOrProperty(key));
    }

    public static boolean getOverriddenBooleanValue(String key, boolean defaultValue) {
        return isKeyProvidedInEnvOrProperty(key)
                ? Boolean.parseBoolean(getValueFromEnvOrProperty(key))
                : defaultValue;
    }

    private static boolean isKeyProvidedInEnvOrProperty(String key) {
        return (null != System.getenv(key)) || (null != System.getProperty(key));
    }

    private static String getValueFromEnvOrProperty(String key) {
        return (null == System.getenv(key)) ? System.getProperty(key) : System.getenv(key);
    }
}
