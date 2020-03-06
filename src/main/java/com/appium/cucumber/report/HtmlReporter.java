package com.appium.cucumber.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.appium.filelocations.FileLocations;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.velocity.exception.VelocityException;

public class HtmlReporter {
    public File         reportOutputDirectory = new File(
        System.getProperty("user.dir") + FileLocations.OUTPUT_DIRECTORY);
    public List<String> list                  = new ArrayList<String>();

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else if (fileEntry.getName()
                .endsWith(".json")) {
                System.out.println("*******" + fileEntry.getName());
                list.add(reportOutputDirectory + "/" + fileEntry.getName());
            }
        }
    }

    public void generateReports() throws VelocityException, IOException {
        listFilesForFolder(reportOutputDirectory);
        String jenkinsBasePath = "";
        String buildNumber = "1";
        String projectName = "cucumber-jvm";

        Configuration configuration = new Configuration(reportOutputDirectory, projectName);
        configuration.setBuildNumber(buildNumber);

        ReportBuilder reportBuilder = new ReportBuilder(list, configuration);
        reportBuilder.generateReports();
    }

}
