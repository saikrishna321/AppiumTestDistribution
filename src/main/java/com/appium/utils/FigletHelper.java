package com.appium.utils;

import java.io.IOException;

import com.github.lalyos.jfiglet.FigletFont;

public class FigletHelper {
    public static void figlet(String text) {
        String asciiArt1 = null;
        try {
            asciiArt1 = FigletFont.convertOneLine(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(asciiArt1);
    }
}
