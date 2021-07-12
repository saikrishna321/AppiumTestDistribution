package com.appium.utils;

import com.github.lalyos.jfiglet.FigletFont;
import org.apache.log4j.Logger;

import java.io.IOException;

public class FigletHelper {
    private static final Logger LOGGER = Logger.getLogger(FigletHelper.class.getName());

    public static void figlet(String text) {
        String asciiArt1 = null;
        try {
            asciiArt1 = FigletFont.convertOneLine(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info(asciiArt1);
    }
}
