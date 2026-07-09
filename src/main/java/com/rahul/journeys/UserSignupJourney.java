package com.rahul.journeys;

import com.rahul.framework.core.Journey;
import com.rahul.framework.core.JourneyContext;
import com.rahul.pages.HomePage;
import com.rahul.pages.SignupLoginPage;

import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Complete business flow: register a brand-new user, verify they are logged
 * in, then delete the account so the journey is repeatable and leaves no
 * state behind. Fully self-contained — safe to run in parallel with anything.
 */
public class UserSignupJourney implements Journey {

    @Override
    public String name() {
        return "User signup, login verification and account deletion";
    }

    @Override
    public void run(JourneyContext ctx) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String username = "rahul-" + unique;
        String email = "rahul.test." + unique + "@example.com";

        HomePage home = new HomePage(ctx.page());
        SignupLoginPage signup = new SignupLoginPage(ctx.page());

        ctx.step("Open home page and go to Signup / Login");
        home.navigate().goToSignupLogin();

        ctx.step("Start signup as " + username + " <" + email + ">");
        signup.startSignup(username, email);

        ctx.step("Fill account details and create account");
        signup.fillAccountDetailsAndCreate("S3cret!" + unique);
        assertThat(signup.accountCreatedBanner()).isVisible();
        signup.continueAfterAccountCreated();

        ctx.step("Verify header shows 'Logged in as " + username + "'");
        assertThat(home.loggedInAsBanner()).containsText(username);

        ctx.step("Delete the account to leave no state behind");
        home.deleteAccount();
        assertThat(signup.accountDeletedBanner()).isVisible();
        signup.continueAfterAccountDeleted();
    }
}
