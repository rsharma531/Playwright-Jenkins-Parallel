package com.rahul.framework.core;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.rahul.framework.config.FrameworkConfig;
import com.rahul.framework.reporting.ExtentReportManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * The worker layer of the two-tier parallelism model.
 *
 * Tier 1 (shard layer, Jenkins): each agent runs this same engine with a
 * different SHARD_INDEX, so each JVM only ever sees its own slice of the
 * manifest (see JourneyManifest).
 *
 * Tier 2 (worker layer, this class): the shard's journeys go into one shared
 * ConcurrentLinkedQueue. A ForkJoinPool of WORKERS threads runs one worker
 * loop per thread; each loop poll()s the next journey, runs it end-to-end,
 * and comes back for another. Nothing is pre-assigned to a thread, so a
 * thread that drew three quick journeys naturally absorbs more work while a
 * thread stuck on a slow checkout keeps grinding — the queue self-balances.
 *
 * Per journey: fresh BrowserContext (from the thread's own browser, via the
 * ThreadLocal BrowserFactory) -> start tracing -> run the business flow ->
 * on pass, discard the trace; on failure, save trace.zip + screenshot and
 * attach both to the Extent report.
 */
public class JourneyEngine {

    public static final Path TRACES_DIR = Paths.get("target", "traces");

    public List<JourneyResult> execute() {
        List<Journey> journeys = JourneyManifest.loadForThisShard();
        Queue<Journey> queue = new ConcurrentLinkedQueue<>(journeys);
        List<JourneyResult> results = Collections.synchronizedList(new ArrayList<>());

        int workers = Math.max(1, Math.min(FrameworkConfig.workers(), journeys.size()));
        System.out.printf("Starting ForkJoinPool with %d workers for %d journeys%n",
                workers, journeys.size());

        ForkJoinPool pool = new ForkJoinPool(workers);
        try {
            List<ForkJoinTask<?>> workerLoops = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                workerLoops.add(pool.submit(() -> workerLoop(queue, results)));
            }
            workerLoops.forEach(ForkJoinTask::join);
        } finally {
            pool.shutdown();
            ExtentReportManager.flush();
        }
        return results;
    }

    /** One of these runs per worker thread: pull, run, repeat until empty. */
    private void workerLoop(Queue<Journey> queue, List<JourneyResult> results) {
        try {
            Journey journey;
            while ((journey = queue.poll()) != null) {
                results.add(runOne(journey));
            }
        } finally {
            // Playwright objects must be closed by their owning thread.
            BrowserFactory.closeCurrentThread();
        }
    }

    private JourneyResult runOne(Journey journey) {
        String thread = Thread.currentThread().getName();
        System.out.printf("[%s] >>> %s%n", thread, journey.name());

        ExtentTest report = ExtentReportManager.createTest(journey.name());
        report.info("Running on thread: " + thread);

        long start = System.currentTimeMillis();
        try (BrowserContext context = BrowserFactory.newContext()) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));
            Page page = context.newPage();
            try {
                journey.run(new JourneyContext(page, report));
                context.tracing().stop(); // passed: discard the trace
                long millis = System.currentTimeMillis() - start;
                report.pass("Journey completed in " + millis + " ms");
                System.out.printf("[%s] PASS %s (%d ms)%n", thread, journey.name(), millis);
                return JourneyResult.pass(journey.name(), millis);
            } catch (Throwable failure) {
                long millis = System.currentTimeMillis() - start;
                Path trace = saveTrace(context, journey.name());
                attachFailure(report, page, failure, trace);
                System.out.printf("[%s] FAIL %s (%d ms): %s%n",
                        thread, journey.name(), millis, failure.getMessage());
                return JourneyResult.fail(journey.name(), millis, failure, trace);
            }
        }
    }

    private Path saveTrace(BrowserContext context, String journeyName) {
        try {
            Files.createDirectories(TRACES_DIR);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            Path trace = TRACES_DIR.resolve(sanitize(journeyName) + "-" + stamp + ".zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(trace));
            return trace;
        } catch (Exception e) {
            System.err.println("Could not save trace for " + journeyName + ": " + e.getMessage());
            return null;
        }
    }

    private void attachFailure(ExtentTest report, Page page, Throwable failure, Path trace) {
        report.fail(failure);
        try {
            String screenshot = Base64.getEncoder().encodeToString(page.screenshot());
            report.fail("Screenshot at failure",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(screenshot).build());
        } catch (Exception e) {
            report.warning("Could not capture screenshot: " + e.getMessage());
        }
        if (trace != null) {
            report.fail("Trace saved: <b>" + trace + "</b> — open it with "
                    + "<code>npx playwright show-trace " + trace.getFileName() + "</code> "
                    + "or at <a href='https://trace.playwright.dev'>trace.playwright.dev</a>");
        }
    }

    private static String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }
}
