package com.appium.webtest;

import com.test.site.UserBaseTest;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

public class LoginTest extends UserBaseTest {
    @Test
    public void loginTest() throws InterruptedException {
        getDriver().get("http://www.thoughtworks.com");
        WebElement idElement = getDriver().findElement(By.id("mobile-menu-title"));
        Assert.assertNotNull(idElement);
        idElement.click();
        WebElement commentElement = getDriver().findElement(
            By.xpath(".//*[@id='mobile-menu']/li[6]/a"));
        Assert.assertNotNull(commentElement);
        commentElement.click();
        Thread.sleep(5000);
        WebElement contact_us = getDriver().findElement(
            By.xpath(".//*[@id='footer-menu']/div/ul/li[9]/a"));
        contact_us.click();
    }
}