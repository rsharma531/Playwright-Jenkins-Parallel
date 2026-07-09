package com.rahul.tests;

import com.rahul.framework.core.JourneyEngine;
import com.rahul.framework.core.JourneyResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The single TestNG entry point. TestNG/Surefire is deliberately just the
 * launcher — parallelism lives inside JourneyEngine (ForkJoinPool), not in
 * TestNG's thread model, so the engine behaves identically under Maven,
 * an IDE run, or Jenkins. Per-journey detail lives in the Extent report;
 * this test fails if any journey failed, which is what gates the CI stage.
 */
public class JourneySuiteTest {

    @Test
    public void runAllJourneysForThisShard() {
        List<JourneyResult> results = new JourneyEngine().execute();

        System.out.println("\n================ JOURNEY SUMMARY ================");
        for (JourneyResult r : results) {
            System.out.printf("%-6s %-60s %6d ms  [%s]%n",
                    r.passed() ? "PASS" : "FAIL", r.journeyName(), r.durationMillis(), r.threadName());
        }
        System.out.println("=================================================\n");

        List<JourneyResult> failures = results.stream().filter(r -> !r.passed()).toList();
        if (!failures.isEmpty()) {
            String detail = failures.stream()
                    .map(f -> "  - " + f.journeyName() + ": " + f.failure()
                            + (f.tracePath() != null ? " (trace: " + f.tracePath() + ")" : ""))
                    .collect(Collectors.joining("\n"));
            Assert.fail(failures.size() + " of " + results.size() + " journeys failed:\n" + detail
                    + "\nSee target/extent-report/extent-report.html and target/traces/");
        }
    }
}
