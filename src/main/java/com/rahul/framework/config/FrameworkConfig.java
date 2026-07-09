package com.rahul.framework.config;

/**
 * Central configuration. Every knob is an environment variable (or -D system
 * property, which wins) so the SAME build runs unchanged on a laptop and on a
 * Jenkins agent — only the environment differs.
 *
 *   BASE_URL     target application            (default: https://automationexercise.com)
 *   BROWSER      chromium | firefox | webkit   (default: chromium)
 *   HEADLESS     true | false                  (default: true)
 *   WORKERS      journey threads per JVM       (default: 2)
 *   SHARD_INDEX  this agent's shard number     (default: 0)
 *   SHARD_TOTAL  total Jenkins shards          (default: 1)
 */
public final class FrameworkConfig {

    private FrameworkConfig() {
    }

    public static String baseUrl() {
        return get("BASE_URL", "https://automationexercise.com");
    }

    public static String browser() {
        return get("BROWSER", "chromium");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("HEADLESS", "true"));
    }

    public static int workers() {
        return Integer.parseInt(get("WORKERS", "2"));
    }

    public static int shardIndex() {
        return Integer.parseInt(get("SHARD_INDEX", "0"));
    }

    public static int shardTotal() {
        return Integer.parseInt(get("SHARD_TOTAL", "1"));
    }

    private static String get(String key, String defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        String env = System.getenv(key);
        return (env != null && !env.isBlank()) ? env : defaultValue;
    }
}
