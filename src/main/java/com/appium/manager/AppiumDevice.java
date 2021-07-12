package com.appium.manager;

import com.appium.entities.MobilePlatform;
import com.appium.filelocations.FileLocations;
import com.github.device.Device;
import com.video.recorder.AppiumScreenRecordFactory;
import com.video.recorder.IScreenRecord;
import org.apache.log4j.Logger;
import org.openqa.selenium.logging.LogEntries;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.StreamSupport;

public class AppiumDevice {

    private static final Logger LOGGER = Logger.getLogger(AppiumDevice.class.getName());
    private static final String AVAILABLE = "AVAILABLE";
    private static final String BUSY = "BUSY";
    private final Device device;
    private int port;
    private int chromeDriverPort;
    private String chromeDriverExecutable;
    private String webkitProcessID;
    private String deviceState;
    private final String ipAddress;

    public AppiumDevice(Device device, String ipAddress) {
        this.device = device;
        this.ipAddress = ipAddress;
        deviceState = AVAILABLE;
        chromeDriverPort = 0; //Setting as Zero initially
    }

    @Override
    public String toString() {
        String deviceInfo = device.toString();
        deviceInfo += " :: Is Available? : " + isAvailable();
        return deviceInfo;
    }

    public int getChromeDriverPort() {
        return chromeDriverPort;
    }

    public String getChromeDriverExecutable() {
        return chromeDriverExecutable;
    }

    public void setChromeDriverExecutable(String chromeDriverExecutable) {
        this.chromeDriverExecutable = chromeDriverExecutable;
    }

    public void setChromeDriverPort(int chromeDriverPort) {
        this.chromeDriverPort = chromeDriverPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Device getDevice() {
        return device;
    }

    public String getHostName() {
        return ipAddress;
    }

    public boolean isLocalDevice() {
        return ipAddress.equals("127.0.0.1");
    }

    public boolean isAvailable() {
        return deviceState.equalsIgnoreCase(AVAILABLE);
    }

    public void blockDevice() {
        deviceState = BUSY;
    }

    public void freeDevice() {
        deviceState = AVAILABLE;
    }

    public String getWebkitProcessID() {
        return webkitProcessID;
    }

    public void setWebkitProcessID(String webkitProcessID) {
        this.webkitProcessID = webkitProcessID;
    }

    private boolean isNativeAndroid() {
        return AppiumDeviceManager.getMobilePlatform().equals(MobilePlatform.ANDROID)
                && AppiumDriverManager.getDriver().getCapabilities()
                .getCapability("browserName") == null;
    }

    public String startDataCapture(String scenarioName,
                                   Integer scenarioRunCount)
            throws IOException, InterruptedException {
        String fileName = String.format("/run-%s", scenarioRunCount);
        if (isNativeAndroid()) {
            String udid = this.getDevice().getUdid();
            fileName = String.format("/%s-run-%s", udid, scenarioRunCount);
            File logFile = createFile( FileLocations.REPORTS_DIRECTORY
                    + scenarioName
                    + File.separator
                    + FileLocations.DEVICE_LOGS_DIRECTORY,
                    fileName);
            fileName = logFile.getAbsolutePath();
            PrintStream logFileStream = new PrintStream(logFile);
            try {
                LogEntries logcatOutput = AppiumDriverManager.getDriver()
                        .manage().logs().get("logcat");
                StreamSupport.stream(logcatOutput.spliterator(), false)
                        .forEach(logFileStream::println);
            } catch (Exception e) {
                LOGGER.info("ERROR in getting logcat. Skipping logcat capture");
            }
        }
        return fileName;
    }

    public static File createFile(String dirName, String fileName) {
        File logFile = new File(System.getProperty("user.dir")
                 + dirName
                + fileName + ".txt");
        if (logFile.exists()) {
            return logFile;
        }
        try {
            logFile.getParentFile().mkdirs();
            logFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logFile;
    }
}
