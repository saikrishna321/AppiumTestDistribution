import com.appium.ios.SimManager;
import com.github.manager.AppiumDriverManager;
import org.testng.annotations.Test;

/**
 * Created by saikrisv on 14/08/17.
 */
public class SimulatorParserTest {

    SimManager          simManager;
    AppiumDriverManager appiumDriverManager = new AppiumDriverManager();

    public SimulatorParserTest() throws Exception {
    }

    @Test
    public void parseSimulatorJson() throws Exception {
        String version = "9.1.1";
        Float aFloat = Float.valueOf(version.substring(0, version.length() - 2));
        System.out.println(aFloat);
    }
}
