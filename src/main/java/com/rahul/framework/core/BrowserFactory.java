package com.rahul.framework.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.rahul.framework.config.FrameworkConfig;

import java.util.List;

/**
 * ThreadLocal browser factory — the reason the ForkJoinPool works at all.
 *
 * Playwright Java is NOT thread-safe: a Playwright instance (and everything
 * created from it) must only ever be touched by the thread that created it.
 * So each worker thread lazily creates its own Playwright + Browser the first
 * time it asks for one, keeps them for its whole lifetime (browser launch is
 * expensive — we don't want one per journey), and hands out a cheap fresh
 * BrowserContext per journey for isolation (own cookies/storage, own trace).
 *
 * Lifecycle per worker thread:
 *   first journey  -> create Playwright, launch Browser   (once)
 *   every journey  -> newContext()                        (cheap, isolated)
 *   queue drained  -> closeCurrentThread()                (worker's finally)
 */
public final class BrowserFactory {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();

    /** Third-party noise blocked in every context: faster + far less flaky. */
    private static final List<String> BLOCKED_HOSTS = List.of(
            "googlesyndication.com", "doubleclick.net", "googletagmanager.com",
            "google-analytics.com", "adtrafficquality.google", "googleadservices.com");

    private BrowserFactory() {
    }

    /** Fresh, isolated context for one journey. Caller must close it. */
    public static BrowserContext newContext() {
        BrowserContext context = browserForThisThread().newContext(
                new Browser.NewContextOptions().setViewportSize(1440, 900));
        context.setDefaultTimeout(30_000);
        context.setDefaultNavigationTimeout(60_000);
        context.route("**/*", route -> {
            String url = route.request().url();
            if (BLOCKED_HOSTS.stream().anyMatch(url::contains)) {
                route.abort();
            } else {
                route.resume();
            }
        });
        return context;
    }

    private static Browser browserForThisThread() {
        Browser browser = BROWSER.get();
        if (browser == null) {
            Playwright playwright = Playwright.create();
            PLAYWRIGHT.set(playwright);
            BrowserType type = switch (FrameworkConfig.browser().toLowerCase()) {
                case "firefox" -> playwright.firefox();
                case "webkit" -> playwright.webkit();
                default -> playwright.chromium();
            };
            browser = type.launch(new BrowserType.LaunchOptions()
                    .setHeadless(FrameworkConfig.headless()));
            BROWSER.set(browser);
            System.out.printf("[%s] launched %s (headless=%s)%n",
                    Thread.currentThread().getName(), FrameworkConfig.browser(),
                    FrameworkConfig.headless());
        }
        return browser;
    }

    /**
     * Must be called by the worker thread itself when it finishes its loop —
     * Playwright objects must be closed on their owning thread.
     */
    public static void closeCurrentThread() {
        Browser browser = BROWSER.get();
        if (browser != null) {
            browser.close();
            BROWSER.remove();
        }
        Playwright playwright = PLAYWRIGHT.get();
        if (playwright != null) {
            playwright.close();
            PLAYWRIGHT.remove();
        }
    }
}
