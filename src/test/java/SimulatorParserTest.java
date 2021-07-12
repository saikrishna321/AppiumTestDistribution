import org.apache.log4j.Logger;
import org.testng.annotations.Test;

public class SimulatorParserTest {
    private static final Logger LOGGER = Logger.getLogger(SimulatorParserTest.class.getName());

    @Test
    public void parseSimulatorJson() {
        String version = "9.1.1";
        Float aFloat = Float.valueOf(version.substring(0, version.length() - 2));
        LOGGER.info(aFloat);
    }
}
