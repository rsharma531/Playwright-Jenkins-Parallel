package com.rahul.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.rahul.framework.config.FrameworkConfig;

/**
 * One ExtentReports instance per JVM (i.e. per shard). createTest() and
 * flush() are thread-safe in Extent 5, and each journey logs only into its
 * own ExtentTest node, so concurrent journeys never interleave their steps.
 * Each Jenkins shard produces its own HTML report, archived per shard.
 */
public final class ExtentReportManager {

    public static final String REPORT_DIR = "target/extent-report";

    private static final ExtentReports EXTENT = create();

    private ExtentReportManager() {
    }

    private static ExtentReports create() {
        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_DIR + "/extent-report.html");
        spark.config().setDocumentTitle("Journey Suite — shard "
                + FrameworkConfig.shardIndex() + "/" + FrameworkConfig.shardTotal());
        spark.config().setReportName("Playwright Journey Framework");
        spark.config().setTheme(Theme.STANDARD);

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Base URL", FrameworkConfig.baseUrl());
        extent.setSystemInfo("Browser", FrameworkConfig.browser());
        extent.setSystemInfo("Headless", String.valueOf(FrameworkConfig.headless()));
        extent.setSystemInfo("Workers (ForkJoinPool)", String.valueOf(FrameworkConfig.workers()));
        extent.setSystemInfo("Shard", FrameworkConfig.shardIndex() + " of " + FrameworkConfig.shardTotal());
        return extent;
    }

    public static ExtentTest createTest(String journeyName) {
        return EXTENT.createTest(journeyName);
    }

    public static void flush() {
        EXTENT.flush();
        System.out.println("Extent report written to " + REPORT_DIR + "/extent-report.html");
    }
}
