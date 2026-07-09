package com.rahul.framework.core;

import com.aventstack.extentreports.ExtentTest;
import com.microsoft.playwright.Page;

/**
 * Everything a journey needs at runtime: its own Page (from its own
 * BrowserContext) and its own ExtentTest node. Handed to the journey by the
 * engine — journeys never create browsers or report nodes themselves.
 */
public class JourneyContext {

    private final Page page;
    private final ExtentTest report;

    public JourneyContext(Page page, ExtentTest report) {
        this.page = page;
        this.report = report;
    }

    public Page page() {
        return page;
    }

    /** Log a business-level step into the Extent report (and the console). */
    public void step(String description) {
        report.info(description);
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), description);
    }

    ExtentTest report() {
        return report;
    }
}
