package com.video.recorder;

import com.appium.filelocations.FileLocations;
import com.appium.manager.AppiumDeviceManager;
import com.appium.utils.CommandPrompt;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by saikrisv on 2016/09/23.
 */
public class Flick extends CommandPrompt implements IScreenRecord {
    private static final Logger LOGGER = Logger.getLogger(Flick.class.getName());

    /**
     * @param className     - Current test class name
     * @param methodName    - Current test method name
     * @param videoFileName - filename should have TimeStamp,deviceID with TestMethod name
     * @throws IOException  -
     * @throws InterruptedException -
     */
    public void stopVideoRecording(String className, String methodName,
                                   String videoFileName) throws IOException, InterruptedException {
        LOGGER.info("**************Stopping Video Recording**************");
        flickRecordingCommand("stop", className, methodName, videoFileName);

    }

    /**
     * @param className     - Current test class name
     * @param methodName    - Current test method name
     * @param videoFileName - filename should have TimeStamp,deviceID with TestMethod name
     * @throws IOException  -
     * @throws InterruptedException -
     */
    public void startVideoRecording(String className, String methodName,
                                    String videoFileName) throws IOException, InterruptedException {
        LOGGER.info("**************Starting Video Recording**************");
        flickRecordingCommand("start", className, methodName, videoFileName);
    }

    private void flickRecordingCommand(String command, String className,
                                       String methodName, String videoFileName)
            throws IOException, InterruptedException {
        String videoPath = System.getProperty("user.dir");
        String android;
        String ios;
        if (AppiumDeviceManager.getAppiumDevice().getDevice().getUdid().length() != 40) {
            String videoLocationAndroid =
                    videoPath + FileLocations.ANDROID_SCREENSHOTS_DIRECTORY
                            + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid() + "/"
                            + className + "/" + methodName;
            fileDirectoryCheck(videoLocationAndroid);
            if (command.equals("start")) {
                try {
                    android = "flick video -a " + command + " -p android -u "
                            + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid();
                    runCommandThruProcess(android);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                android = "flick video -a " + command + " -p android -o "
                        + videoLocationAndroid + " -n " + videoFileName
                        + " -u " + AppiumDeviceManager.getAppiumDevice()
                        .getDevice().getUdid() + " --trace";
                runCommandThruProcess(android);
                LOGGER.info("Stopping Video recording on Emulator");
                Thread.sleep(10000);

            }
        } else {
            String videoLocationIOS =
                    videoPath + FileLocations.IOS_SCREENSHOTS_DIRECTORY
                            + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid()
                            + "/" + className + "/" + methodName;
            fileDirectoryCheck(videoLocationIOS);
            if (command.equals("start")) {
                ios = "flick video -a " + command + " -p ios -u "
                        + AppiumDeviceManager.getAppiumDevice().getDevice().getUdid();
            } else {
                ios = "flick video -a " + command + " -p ios -o " + videoLocationIOS + " -n "
                        + videoFileName + " -u " + AppiumDeviceManager.getAppiumDevice()
                        .getDevice().getUdid();
            }

            LOGGER.info(ios);
            try {
                runCommand(ios);
                Thread.sleep(5000);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fileDirectoryCheck(String folderPath) {
        File f = new File(folderPath);
        f.mkdirs();
    }
}
