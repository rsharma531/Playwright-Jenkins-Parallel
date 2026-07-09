package com.rahul.pages;

import com.microsoft.playwright.Page;
import com.rahul.framework.config.FrameworkConfig;

/**
 * Page objects wrap locators and user actions for one screen. They receive
 * the journey's Page — they never create browsers or touch the engine, so
 * they stay reusable across journeys and oblivious to parallelism.
 */
public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    protected void open(String path) {
        page.navigate(FrameworkConfig.baseUrl() + path);
    }
}
