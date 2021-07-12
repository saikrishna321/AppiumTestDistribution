import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.log4j.Logger;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;

import static com.appium.manager.AppiumDriverManager.getDriver;


public class MyStepdefs {
    RemoteWebElement edit;
    private static final Logger LOGGER = Logger.getLogger(MyStepdefs.class.getName());

    @When("I click on {int} number")
    public void iClickOnNumber(int arg0) {
        LOGGER.info("I click on {int} number : " + arg0);
        edit = (RemoteWebElement) getDriver().findElementByClassName("Edit");
    }

    @Given("I accept the tip screen")
    public void i_accept_the_tip_screen() {
        // Write code here that turns the phrase above into concrete actions
        LOGGER.info("I accept the tip screen step executed ");
    }
}
