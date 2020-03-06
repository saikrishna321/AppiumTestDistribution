package com.appium.executor;

public class TestNGTestInfo {
    private String testClassName;
    private String testMethodName;
    private String testgroups;

    public TestNGTestInfo(String testClassName, String testMethodName, String testgroups) {
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.testgroups = testgroups;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public void setTestClassName(String testClassName) {
        this.testClassName = testClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    public String getTestgroups() {
        return testgroups;
    }

    public void setTestgroups(String testgroups) {
        this.testgroups = testgroups;
    }
}
