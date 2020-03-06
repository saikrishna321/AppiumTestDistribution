package com.appium.executor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.appium.filelocations.FileLocations;
import com.github.device.HostMachineDeviceManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;

public class MyTestExecutorTest {
    MyTestExecutor ex1;

    public MyTestExecutorTest() throws Exception {
        ex1 = new MyTestExecutor();
    }

    public void testXmlSuiteCreation() {
        ArrayList<String> devices = new ArrayList<>();
        devices.add("192.168.0.1");
        devices.add("192.168.0.2");
        devices.add("192.168.0.3");
        devices.add("192.168.0.4");
        Method[] thizMethods = MyTestExecutorTest.class.getMethods();
        Set<Method> methods = new HashSet<>(Arrays.asList(thizMethods));

        Method[] otherMethods = OtherTests.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods));

        Method[] otherMethods1 = OtherTests1.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods1));
        List<String> tc = new ArrayList<>();

        String suiteName = "TestNG Forum";
        String category = "TestNG Test";

        XmlSuite xmlSuite = ex1.constructXmlSuiteForDistribution(tc, ex1.createTestsMap(methods),
            suiteName, category, devices.size());
        System.out.println("xml:" + xmlSuite.toXml());
        Assert.assertTrue(true);
    }

    @Test
    public void testXmlSuiteCreationForMethodParallelCucumber() throws IOException {
        ArrayList<String> devices = new ArrayList<>();
        devices.add("192.168.0.1");
        devices.add("192.168.0.2");
        devices.add("192.168.0.3");
        devices.add("192.168.0.4");
        Method[] thizMethods = MyTestExecutorTest.class.getMethods();
        Set<Method> methods = new HashSet<>(Arrays.asList(thizMethods));

        Method[] otherMethods = OtherTests.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods));

        Method[] otherMethods1 = OtherTests1.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods1));
        List<String> tc = new ArrayList<>();

        String suiteName = "TestNG Forum";
        String category = "TestNG Test";

        XmlSuite xmlSuite = ex1.constructXmlSuiteForDistributionMethods(tc,
            ex1.createTestsMap(methods), suiteName, category, devices.size());
        System.out.println("xml:" + xmlSuite.toXml());
        Assert.assertTrue(true);
    }

    @Test
    public void testXmlSuiteCreationForMethodParallel() throws IOException {
        ArrayList<String> devices = new ArrayList<>();
        devices.add("192.168.0.1");
        devices.add("192.168.0.2");
        devices.add("192.168.0.3");
        devices.add("192.168.0.4");
        Method[] thizMethods = MyTestExecutorTest.class.getMethods();
        Set<Method> methods = new HashSet<>(Arrays.asList(thizMethods));

        Method[] otherMethods = OtherTests.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods));

        Method[] otherMethods1 = OtherTests1.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods1));
        List<String> tc = new ArrayList<>();

        String suiteName = "TestNG Forum";
        String category = "TestNG Test";

        XmlSuite xmlSuite = ex1.constructXmlSuiteForParallel("com.appium.executor", tc,
            ex1.createTestsMap(methods), suiteName, category, devices.size(),
            HostMachineDeviceManager.getInstance()
                .getDevicesByHost()
                .getAllDevices());

        System.out.println("xml:" + xmlSuite.toXml());
        Assert.assertTrue(true);
    }

    public void testXmlSuiteCreationCucumber() throws IOException {
        ArrayList<String> devices = new ArrayList<>();
        devices.add("192.168.0.1");
        devices.add("192.168.0.2");
        devices.add("192.168.0.3");
        devices.add("192.168.0.4");
        Method[] thizMethods = MyTestExecutorTest.class.getMethods();
        Set<Method> methods = new HashSet<>(Arrays.asList(thizMethods));

        Method[] otherMethods = OtherTests.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods));

        Method[] otherMethods1 = OtherTests1.class.getMethods();
        methods.addAll(Arrays.asList(otherMethods1));
        XmlSuite xmlSuite = ex1.constructXmlSuiteForParallelCucumber(devices.size(),
            HostMachineDeviceManager.getInstance()
                .getDevicesByHost()
                .getAllDevices());
        System.out.println("xml:" + xmlSuite.toXml());
        File file = new File(System.getProperty("user.dir") + FileLocations.PARALLEL_XML_LOCATION);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file.getAbsoluteFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);
        try {
            bw.write(xmlSuite.toXml());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(true);
    }
}
