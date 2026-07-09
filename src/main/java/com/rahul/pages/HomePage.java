package com.rahul.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class HomePage extends BasePage {

    public HomePage(Page page) {
        super(page);
    }

    public HomePage navigate() {
        open("/");
        assertThat(page.locator("#header")).isVisible();
        return this;
    }

    public void goToSignupLogin() {
        page.locator("#header a[href='/login']").click();
    }

    public void goToProducts() {
        page.locator("#header a[href='/products']").click();
    }

    public Locator loggedInAsBanner() {
        return page.locator("#header li", new Page.LocatorOptions().setHasText("Logged in as"));
    }

    public void deleteAccount() {
        page.locator("#header a[href='/delete_account']").click();
    }

    public void subscribeToNewsletter(String email) {
        Locator emailField = page.locator("#susbscribe_email"); // typo is in the site's own id
        emailField.scrollIntoViewIfNeeded();
        emailField.fill(email);
        page.locator("#subscribe").click();
    }

    public Locator subscriptionSuccessAlert() {
        return page.locator("#success-subscribe .alert-success");
    }
}
