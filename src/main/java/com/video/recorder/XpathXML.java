package com.video.recorder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import com.appium.filelocations.FileLocations;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Created by saikrisv on 2016/11/07.
 */
public class XpathXML {
    public static final String PARALLEL_FILE_LOCATION = FileLocations.PARALLEL_XML_LOCATION;

    public String parseXML(int threadNumber) {
        try {
            File inputFile = new File(System.getProperty("user.dir") + PARALLEL_FILE_LOCATION);
            if (inputFile.exists()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;

                dBuilder = dbFactory.newDocumentBuilder();

                Document doc = dBuilder.parse(inputFile);
                doc.getDocumentElement()
                    .normalize();

                XPath xPath = XPathFactory.newInstance()
                    .newXPath();

                String expression = "/suite/test/parameter";
                NodeList nodeList = (NodeList) xPath.compile(expression)
                    .evaluate(doc, XPathConstants.NODESET);
                String value = nodeList.item(threadNumber)
                    .getAttributes()
                    .getNamedItem("value")
                    .getNodeValue();
                return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
