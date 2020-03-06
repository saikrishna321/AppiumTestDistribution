package com.test.cucumber.stepdefinitions;

import io.appium.java_client.AppiumDriver;

/**
 * Created by saikrisv on 05/04/16.
 */
public class DriverManager {
    public static ThreadLocal<AppiumDriver> driver = new ThreadLocal<>();

    public static AppiumDriver getDriver() {
        return driver.get();
    }

    public static void setWebDriver(AppiumDriver driver_) {
        driver.set(driver_);
    }
}
