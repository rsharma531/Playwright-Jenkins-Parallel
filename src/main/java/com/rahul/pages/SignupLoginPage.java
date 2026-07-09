package com.rahul.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SignupLoginPage extends BasePage {

    public SignupLoginPage(Page page) {
        super(page);
    }

    public void startSignup(String name, String email) {
        assertThat(page.locator("input[data-qa='signup-name']")).isVisible();
        page.locator("input[data-qa='signup-name']").fill(name);
        page.locator("input[data-qa='signup-email']").fill(email);
        page.locator("button[data-qa='signup-button']").click();
    }

    public void fillAccountDetailsAndCreate(String password) {
        assertThat(page.locator("input[data-qa='password']")).isVisible();
        page.locator("#id_gender1").check();
        page.locator("input[data-qa='password']").fill(password);
        page.locator("select[data-qa='days']").selectOption(new SelectOption().setValue("10"));
        page.locator("select[data-qa='months']").selectOption(new SelectOption().setValue("5"));
        page.locator("select[data-qa='years']").selectOption(new SelectOption().setValue("1995"));
        page.locator("input[data-qa='first_name']").fill("Rahul");
        page.locator("input[data-qa='last_name']").fill("Tester");
        page.locator("input[data-qa='address']").fill("221B Test Street");
        page.locator("select[data-qa='country']").selectOption(new SelectOption().setValue("India"));
        page.locator("input[data-qa='state']").fill("Karnataka");
        page.locator("input[data-qa='city']").fill("Bengaluru");
        page.locator("input[data-qa='zipcode']").fill("560001");
        page.locator("input[data-qa='mobile_number']").fill("9999999999");
        page.locator("button[data-qa='create-account']").click();
    }

    public Locator accountCreatedBanner() {
        return page.locator("h2[data-qa='account-created']");
    }

    public void continueAfterAccountCreated() {
        page.locator("a[data-qa='continue-button']").click();
    }

    public Locator accountDeletedBanner() {
        return page.locator("h2[data-qa='account-deleted']");
    }

    public void continueAfterAccountDeleted() {
        page.locator("a[data-qa='continue-button']").click();
    }
}
