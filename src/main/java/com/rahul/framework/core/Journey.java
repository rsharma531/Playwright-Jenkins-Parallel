package com.rahul.framework.core;

/**
 * A journey is one complete business flow (signup, checkout, search...) and
 * the framework's unit of parallelism: journeys never share state, so any
 * number of them can run concurrently, and one journey's steps always run
 * sequentially on one thread against one BrowserContext.
 */
public interface Journey {

    /** Human-readable name shown in reports and logs. */
    String name();

    /** The business flow. Throw (assertion or otherwise) to fail the journey. */
    void run(JourneyContext ctx);
}
