package com.test.site;

import com.github.manager.AppiumDriverManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class UserBaseTest {
    public AppiumDriver<MobileElement> driver;

    public AppiumDriver<MobileElement> getDriver() {
        driver = AppiumDriverManager.getDriver();
        return driver;
    }

    public WebElement login(String locator) {
        return waitForElement(locator);
    }

    public WebElement waitForElement(String locator) {
        return new WebDriverWait(getDriver(), 30).until(
            ExpectedConditions.elementToBeClickable(MobileBy.AccessibilityId(locator)));
    }
}
