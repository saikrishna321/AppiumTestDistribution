package com.appium.executor;

import static com.appium.filelocations.FileLocations.PARALLEL_XML_LOCATION;
import static com.appium.utils.FigletHelper.figlet;
import static com.github.utils.ConfigFileManager.CATEGORY;
import static com.github.utils.ConfigFileManager.EXCLUDE_GROUPS;
import static com.github.utils.ConfigFileManager.INCLUDE_GROUPS;
import static com.github.utils.ConfigFileManager.LISTENERS;
import static com.github.utils.ConfigFileManager.RUNNER_LEVEL;
import static com.github.utils.ConfigFileManager.SUITE_NAME;
import static java.lang.System.getProperty;
import static java.util.Collections.addAll;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.manager.AppiumDevice;
import com.github.manager.DeviceAllocationManager;
import com.github.utils.ConfigFileManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jakewharton.fliptables.FlipTableConverters;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.ParallelMode;
import org.testng.xml.XmlTest;

public class MyTestExecutor {
    private final DeviceAllocationManager deviceAllocationManager;
    private       List<String>            items         = new ArrayList<String>();
    private       List<String>            listeners     = new ArrayList<>();
    private       List<String>            groupsInclude = new ArrayList<>();
    private       List<String>            groupsExclude = new ArrayList<>();

    public MyTestExecutor() {
        deviceAllocationManager = DeviceAllocationManager.getInstance();
    }

    public boolean runMethodParallelAppium(List<String> test, String pack, int devicecount,
        String executionType) throws Exception {
        Set<Method> resources = getMethods(pack);
        boolean hasFailure;
        dryRunTestInfo(resources);

        String runnerLevel = RUNNER_LEVEL.get();

        if (executionType.equalsIgnoreCase("distribute")) {
            if (runnerLevel != null && runnerLevel.equalsIgnoreCase("class")) {
                constructXmlSuiteForDistribution(test, createTestsMap(resources), getSuiteName(),
                    getCategoryName(), devicecount);
            } else {
                constructXmlSuiteForDistributionMethods(test, createTestsMap(resources),
                    getSuiteName(), getCategoryName(), devicecount);
            }
            hasFailure = runMethodParallel();
        } else {
            constructXmlSuiteForParallel(pack, test, createTestsMap(resources), getSuiteName(),
                getCategoryName(), devicecount, deviceAllocationManager.getDevices());
            hasFailure = runMethodParallel();
        }
        System.out.println("Finally complete");
        figlet("Test Completed");
        return hasFailure;
    }

    private Set<Method> getMethods(String pack) throws MalformedURLException {
        URL newUrl;
        List<URL> newUrls = new ArrayList<>();
        addAll(items, pack.split("\\s*,\\s*"));
        int a = 0;
        Collection<URL> urls = ClasspathHelper.forPackage(items.get(a));
        Iterator<URL> iter = urls.iterator();

        URL url = null;

        while (iter.hasNext()) {
            url = iter.next();
            if (url.toString()
                .contains("test-classes")) {
                break;
            }
        }
        for (String item : items) {
            newUrl = new URL(url.toString() + item.replaceAll("\\.", "/"));
            newUrls.add(newUrl);
            a++;
        }
        Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(newUrls)
            .setScanners(new MethodAnnotationsScanner()));
        return reflections.getMethodsAnnotatedWith(Test.class);
    }

    private void dryRunTestInfo(Set<Method> resources) {
        if (getProperty("dry-run") != null) {
            Multimap<String, Map<String, String>> dryRunResults = ArrayListMultimap.create();
            resources.forEach(method -> {
                HashMap<String, String> methodAndGroups = new HashMap<>();
                String className = method.getDeclaringClass()
                    .getSimpleName();
                String methodName = method.getName();
                String[] groups = method.getAnnotation(Test.class)
                    .groups();
                methodAndGroups.put(methodName, Arrays.toString(groups));
                dryRunResults.put(className, methodAndGroups);
            });

            List<TestNGTestInfo> testInfo = new ArrayList<>();
            for (Map.Entry<String, Map<String, String>> stringObjectEntry :
                dryRunResults.entries()) {
                stringObjectEntry.getValue()
                    .forEach((key, value) -> testInfo.add(
                        new TestNGTestInfo(stringObjectEntry.getKey(), key, value)));
            }
            System.out.println(FlipTableConverters.fromIterable(testInfo, TestNGTestInfo.class));
            System.exit(1);
        }
    }

    public boolean runMethodParallel() {
        TestNG testNG = new TestNG();
        List<String> suites = Lists.newArrayList();
        suites.add(getProperty("user.dir") + PARALLEL_XML_LOCATION);
        testNG.setTestSuites(suites);
        testNG.run();
        return testNG.hasFailure();
    }

    public XmlSuite constructXmlSuiteForParallel(String pack, List<String> testcases,
        Map<String, List<Method>> methods, String suiteName, String category, int deviceCount,
        List<AppiumDevice> deviceSerail) {
        ArrayList<String> listeners = new ArrayList<>();
        listeners.add("com.appium.manager.AppiumParallelTestListener");
        listeners.add("com.appium.utils.RetryListener");
        include(listeners, LISTENERS);
        include(groupsInclude, INCLUDE_GROUPS);
        include(groupsExclude, EXCLUDE_GROUPS);
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        suite.setThreadCount(deviceCount);
        suite.setDataProviderThreadCount(deviceCount);
        suite.setParallel(ParallelMode.TESTS);
        suite.setVerbose(2);
        suite.setListeners(listeners);
        for (int i = 0; i < deviceCount; i++) {
            XmlTest test = new XmlTest(suite);
            test.setName(category + "-" + i);
            test.setPreserveOrder(false);
            test.addParameter("device", deviceSerail.get(i)
                .getDevice()
                .getUdid());
            test.addParameter("hostName", deviceSerail.get(i)
                .getHostName());
            test.setIncludedGroups(groupsInclude);
            test.setExcludedGroups(groupsExclude);
            List<XmlClass> xmlClasses = writeXmlClass(testcases, methods);
            test.setXmlClasses(xmlClasses);
        }
        writeTestNGFile(suite);
        return suite;
    }

    private List<XmlClass> writeXmlClass(List<String> testcases,
        Map<String, List<Method>> methods) {
        List<XmlClass> xmlClasses = new ArrayList<>();
        for (String className : methods.keySet()) {
            if (className.contains("Test")) {
                if (testcases.size() == 0) {
                    xmlClasses.add(createClass(className));
                } else {
                    for (String s : testcases) {
                        for (String item : items) {
                            String testName = item.concat("." + s);
                            if (testName.equals(className)) {
                                xmlClasses.add(createClass(className));
                            }
                        }
                    }
                }
            }
        }
        return xmlClasses;
    }

    public XmlSuite constructXmlSuiteForDistribution(List<String> tests,
        Map<String, List<Method>> methods, String suiteName, String category, int deviceCount) {
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        suite.setThreadCount(deviceCount);
        suite.setParallel(ParallelMode.CLASSES);
        suite.setVerbose(2);
        listeners.add("com.github.manager.AppiumParallelMethodTestListener");
        listeners.add("com.appium.utils.RetryListener");
        include(listeners, LISTENERS);
        suite.setListeners(listeners);
        XmlTest test = new XmlTest(suite);
        test.setName(category);
        test.addParameter("device", "");
        include(groupsExclude, EXCLUDE_GROUPS);
        include(groupsInclude, INCLUDE_GROUPS);
        test.setIncludedGroups(groupsInclude);
        test.setExcludedGroups(groupsExclude);
        List<XmlClass> xmlClasses = writeXmlClass(tests, methods);
        test.setXmlClasses(xmlClasses);
        writeTestNGFile(suite);
        return suite;
    }

    public XmlSuite constructXmlSuiteForDistributionMethods(List<String> tests,
        Map<String, List<Method>> methods, String suiteName, String category, int deviceCount) {
        include(groupsInclude, INCLUDE_GROUPS);
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        suite.setThreadCount(deviceCount);
        suite.setDataProviderThreadCount(deviceCount);
        suite.setVerbose(2);
        suite.setParallel(ParallelMode.METHODS);
        listeners.add("com.github.manager.AppiumParallelMethodTestListener");
        listeners.add("com.appium.utils.RetryListener");
        include(listeners, LISTENERS);
        suite.setListeners(listeners);
        CreateGroups createGroups = new CreateGroups(tests, methods, category, suite).invoke();
        List<XmlClass> xmlClasses = createGroups.getXmlClasses();
        XmlTest test = createGroups.getTest();
        List<XmlClass> writeXml = createGroups.getWriteXml();
        for (XmlClass xmlClass : xmlClasses) {
            writeXml.add(new XmlClass(xmlClass.getName()));
            test.setClasses(writeXml);
        }
        writeTestNGFile(suite);
        return suite;
    }

    private void writeTestNGFile(XmlSuite suite) {
        try (
            FileWriter writer = new FileWriter(
                new File(getProperty("user.dir") + PARALLEL_XML_LOCATION))) {
            writer.write(suite.toXml());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void include(List<String> groupsInclude, ConfigFileManager config) {
        String listItems = config.get();
        if (isNotEmpty(listItems)) {
            addAll(groupsInclude, listItems.split("\\s*,\\s*"));
        }
    }

    private XmlClass createClass(String className) {
        XmlClass clazz = new XmlClass();
        clazz.setName(className);
        return clazz;
    }

    private List<XmlInclude> constructIncludes(List<Method> methods) {
        List<XmlInclude> includes = new ArrayList<>();
        for (Method m : methods) {
            includes.add(new XmlInclude(m.getName()));
        }
        return includes;
    }

    public Map<String, List<Method>> createTestsMap(Set<Method> methods) {
        Map<String, List<Method>> testsMap = new HashMap<>();
        methods.forEach(method -> {
            List<Method> methodsList = testsMap.computeIfAbsent(method.getDeclaringClass()
                .getPackage()
                .getName() + "." + method.getDeclaringClass()
                .getSimpleName(), k -> new ArrayList<>());
            methodsList.add(method);
        });
        return testsMap;
    }

    private void deleteOutputDirectory() {
        File delete_output = new File(getProperty("user.dir") + "/src/test/java/output/");
        File[] files = delete_output.listFiles();

        for (File file : files) {
            file.delete();
        }
    }

    public XmlSuite constructXmlSuiteForParallelCucumber(int deviceCount,
        List<AppiumDevice> deviceSerail) {
        ArrayList<String> listeners = new ArrayList<>();
        listeners.add("com.cucumber.listener.CucumberListener");
        XmlSuite suite = new XmlSuite();
        suite.setName("TestNG Forum");
        suite.setThreadCount(deviceCount);
        suite.setParallel(ParallelMode.TESTS);
        suite.setVerbose(2);
        suite.setListeners(listeners);
        for (int i = 0; i < deviceCount; i++) {
            XmlTest test = new XmlTest(suite);
            test.setName("TestNG Test" + i);
            test.setPreserveOrder(false);
            test.addParameter("device", deviceSerail.get(i)
                .getDevice()
                .getUdid());
            test.setPackages(getPackages());
        }
        return getXmlSuite(suite);
    }

    public XmlSuite constructXmlSuiteDistributeCucumber(int deviceCount) {
        ArrayList<String> listeners = new ArrayList<>();
        listeners.add("com.cucumber.listener.CucumberListener");
        XmlSuite suite = new XmlSuite();
        suite.setName("TestNG Forum");
        suite.setThreadCount(deviceCount);
        suite.setParallel(ParallelMode.CLASSES);
        suite.setVerbose(2);
        suite.setListeners(listeners);
        XmlTest test = new XmlTest(suite);
        test.setName("TestNG Test");
        test.addParameter("device", "");
        test.setPackages(getPackages());
        return getXmlSuite(suite);
    }

    private XmlSuite getXmlSuite(XmlSuite suite) {
        File file = new File(getProperty("user.dir") + PARALLEL_XML_LOCATION);
        try (FileWriter fw = new FileWriter(file.getAbsoluteFile())) {
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(suite.toXml());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return suite;
    }

    private static List<XmlPackage> getPackages() {
        List<XmlPackage> allPackages = new ArrayList<>();
        XmlPackage eachPackage = new XmlPackage();
        eachPackage.setName("output");
        allPackages.add(eachPackage);
        return allPackages;
    }

    private String getSuiteName() {
        return SUITE_NAME.get();
    }

    private String getCategoryName() {
        return CATEGORY.get();
    }

    private class CreateGroups {
        private List<String>              tests;
        private Map<String, List<Method>> methods;
        private String                    category;
        private XmlSuite                  suite;
        private List<XmlClass>            xmlClasses;
        private XmlTest                   test;
        private List<XmlClass>            writeXml;

        public CreateGroups(List<String> tests, Map<String, List<Method>> methods, String category,
            XmlSuite suite) {
            this.tests = tests;
            this.methods = methods;
            this.category = category;
            this.suite = suite;
        }

        public List<XmlClass> getXmlClasses() {
            return xmlClasses;
        }

        public XmlTest getTest() {
            return test;
        }

        public List<XmlClass> getWriteXml() {
            return writeXml;
        }

        public CreateGroups invoke() {
            xmlClasses = writeXmlClass(tests, methods);
            test = new XmlTest(suite);
            test.setName(category);
            test.addParameter("device", "");
            include(groupsExclude, EXCLUDE_GROUPS);
            test.setIncludedGroups(groupsInclude);
            test.setExcludedGroups(groupsExclude);
            writeXml = new ArrayList<>();
            return this;
        }
    }
}