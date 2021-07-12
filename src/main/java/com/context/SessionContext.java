package com.context;

import org.apache.log4j.Logger;
import org.testng.ITestContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

import static com.appium.utils.OverriddenVariable.getOverriddenStringValue;

public class SessionContext {
    static final String TEST_RUNNER = "testrunner";
    private static final HashMap<String, TestExecutionContext> allTestsExecutionContext;
    private static final Logger LOGGER = Logger.getLogger(SessionContext.class.getName());
    private static final Properties reportPortalProperties;
    private static String reportPortalLaunchURL = "";

    static {
        LOGGER.info("SessionContext default constructor");
        new SessionContext();
        allTestsExecutionContext = new HashMap<>();
        reportPortalProperties = loadReportPortalProperties();
        LOGGER.info("Initialized SessionContext");
    }

    static synchronized void addContext(long threadId, TestExecutionContext testExecutionContext) {
        allTestsExecutionContext.put(String.valueOf(threadId), testExecutionContext);
        LOGGER.info(String.format("Adding context for thread - %s", threadId));
    }

    public static synchronized TestExecutionContext getTestExecutionContext(long threadId) {
        return allTestsExecutionContext.get(String.valueOf(threadId));
    }

    public static synchronized void remove(long threadId) {
        LOGGER.info(String.format("Removing context for thread - %s", threadId));
        allTestsExecutionContext.remove(String.valueOf(threadId));
    }

    private static Properties loadReportPortalProperties() {
        Properties properties = new Properties();
        try {
            String reportPortalPropertiesFile = "src/test/resources/reportportal.properties";
            getOverriddenStringValue("REPORT_PORTAL_FILE", reportPortalPropertiesFile);
            LOGGER.info("Using reportportal.properties file from "
                    + reportPortalPropertiesFile);
            File reportPortalFile = new File(reportPortalPropertiesFile);
            String absolutePath = reportPortalFile.getAbsolutePath();
            if (reportPortalFile.exists()) {
                properties.load(new FileInputStream(absolutePath));
                LOGGER.info("Loaded reportportal.properties file - " + absolutePath);
            } else {
                LOGGER.info("reportportal.properties file NOT FOUND - " + absolutePath);
            }

        } catch (IOException e) {
            LOGGER.info("ERROR in loading reportportal.properties file\n" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return properties;
    }

    public static void setReportPortalLaunchURL(ITestContext iTestContext) {
        Optional reportPortalListener = Arrays.stream(iTestContext.getSuite().getXmlSuite()
                .getListeners().toArray()).filter(x ->
                x.equals("com.epam.reportportal.testng.ReportPortalTestNGListener"))
                .findFirst();
        boolean isReportPortalEnabledInProperties =
                (null == reportPortalProperties.getProperty("rp.enable")
                || (reportPortalProperties.getProperty("rp.enable").equalsIgnoreCase("true")));
        if (reportPortalListener.isPresent() && isReportPortalEnabledInProperties) {
            reportPortalLaunchURL = String.format("%s/ui/#%s/launches/all/%s",
                    reportPortalProperties.getProperty("rp.endpoint"),
                    reportPortalProperties.getProperty("rp.project"),
                    System.getProperty("rp.launch.id"));
            LOGGER.info(String.format(
                    "**** ReportPortal URL - %s ****", reportPortalLaunchURL));
        }
    }

    public static String getReportPortalLaunchURL() {
        return reportPortalLaunchURL;
    }
}
