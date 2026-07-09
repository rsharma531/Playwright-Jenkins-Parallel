package com.rahul.framework.core;

import java.nio.file.Path;

/** Outcome of one journey run, aggregated by the engine for the final summary. */
public record JourneyResult(
        String journeyName,
        boolean passed,
        long durationMillis,
        String threadName,
        Throwable failure,
        Path tracePath) {

    static JourneyResult pass(String name, long millis) {
        return new JourneyResult(name, true, millis, Thread.currentThread().getName(), null, null);
    }

    static JourneyResult fail(String name, long millis, Throwable failure, Path tracePath) {
        return new JourneyResult(name, false, millis, Thread.currentThread().getName(), failure, tracePath);
    }
}
