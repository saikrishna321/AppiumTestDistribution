package com.test.site;

/**
 * Created with IntelliJ IDEA. User: haoji Date: 12/17/14 Time: 1:24 PM To change this template use
 * File | Settings | File Templates.
 */
public class MyClass {
    public int sum(int a, int b) {
        if (Math.random() > 0.5) {  // this is a bug that will randomly happen
            return 0;
        }
        return a + b;
    }
}
