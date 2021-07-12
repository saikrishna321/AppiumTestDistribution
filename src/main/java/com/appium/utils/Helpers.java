package com.appium.utils;

import static com.appium.utils.ConfigFileManager.RUNNER;

import com.appium.capabilities.Capabilities;
import com.appium.manager.AppiumDeviceManager;
import com.appium.manager.AppiumParallelMethodTestListener;
import com.appium.manager.AppiumParallelTestListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.report.factory.TestStatusManager;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGListener;
import org.testng.ITestResult;
import org.testng.TestNGException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class Helpers {
    private static final Logger LOGGER = Logger.getLogger(Helpers.class.getName());

    protected String getRemoteAppiumManagerPort(String host) {
        String serverPort = Capabilities.getInstance()
            .getRemoteAppiumManangerPort(host);
        if (serverPort == null) {
            return "4567";
        } else {
            return serverPort;
        }
    }

    protected String getStatus(ITestResult result) {
        switch (result.getStatus()) {
            case ITestResult.SUCCESS:
                return "Pass";
            case ITestResult.FAILURE:
                return "Fail";
            case ITestResult.SKIP:
                return "Skip";
            default:
                return "Unknown";
        }
    }

    protected void queueBeforeInvocationListeners(IInvokedMethod iInvokedMethod,
                                                  ITestResult iTestResult,
                                                  List<ITestNGListener> listeners) {
        for (ITestNGListener listener : listeners) {
            //Lets filter out only IInvokedMethodListener instances.
            if (listener instanceof IInvokedMethodListener) {
                ((IInvokedMethodListener) listener).beforeInvocation(iInvokedMethod, iTestResult);
            }
        }
    }


    protected void queueAfterInvocationListener(IInvokedMethod iInvokedMethod,
                                                ITestResult iTestResult,
                                                List<ITestNGListener> listeners) {
        for (ITestNGListener listener : listeners) {
            //Lets filter out only IInvokedMethodListener instances.
            if (listener instanceof IInvokedMethodListener) {
                ((IInvokedMethodListener) listener).afterInvocation(iInvokedMethod, iTestResult);
            }
        }
    }

    protected List<ITestNGListener> initialiseListeners() {
        List<ITestNGListener> listeners = new LinkedList<>();
        String file = "META-INF/services/listeners.txt";
        InputStream stream = getStream(file);
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    listeners.add(instantiate(line));
                }
            } catch (IOException e) {
                throw new TestNGException(e);
            }
        }
        return listeners;
    }

    private static InputStream getStream(String file) {
        InputStream stream;
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
        if (stream == null) {
            LOGGER.info("Custom Listener not found!!");
        }
        return stream;
    }

    private static ITestNGListener instantiate(String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Please provide a valid class name");
        }
        try {
            Class<?> clazz = Class.forName(className);
            if (!ITestNGListener.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(className
                    + " does not implement a TestNG listener");
            }
            return (ITestNGListener) clazz.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new TestNGException(e);
        }
    }

    public void sendResultsToAtdService(ITestResult testResult,
                                        String testStatus, String url,
                                        HashMap<String, String> logs) {
        String reportEventJson;
        try {
            reportEventJson = new TestStatusManager()
                .getReportEventJson(AppiumDeviceManager.getAppiumDevice(),
                    testStatus,
                    testResult, logs);
            new Api().post(url, reportEventJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public String getHostMachineIpAddress() throws IOException {
        String localHost = InetAddress.getLocalHost().toString();
        if (localHost.contains("/")) {
            return localHost.split("/")[1].trim();
        } else {
            return localHost.trim();
        }
    }

    protected static String getAppiumServerVersion() throws IOException {
        String appiumVersion = null;
        try {
            appiumVersion = new CommandPrompt().runCommand("appium -v")
                .replaceAll("\n", "");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return appiumVersion;
    }

    public boolean isRetry(ITestResult iTestResult) {
        if (iTestResult.getMethod().getRetryAnalyzer(iTestResult) != null) {
            return iTestResult.getMethod().getRetryAnalyzer(iTestResult).retry(iTestResult);
        }
        return false;
    }

    public String getCurrentTestClassName() {
        String runner = RUNNER.get();
        if (runner.equalsIgnoreCase("distribute")) {
            return AppiumParallelMethodTestListener.getTestMethod().getRealClass().getSimpleName();
        } else {
            return AppiumParallelTestListener.getTestMethod().getRealClass().getSimpleName();
        }
    }

    public String getCurrentTestMethodName() {
        String runner = RUNNER.get();
        if (runner.equalsIgnoreCase("distribute")) {
            return AppiumParallelMethodTestListener.getTestMethod().getMethodName();
        } else {
            return AppiumParallelTestListener.getTestMethod().getMethodName();
        }
    }

}
