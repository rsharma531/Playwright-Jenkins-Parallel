package com.rahul.journeys;

import com.rahul.framework.core.Journey;
import com.rahul.framework.core.JourneyContext;
import com.rahul.pages.HomePage;

import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Complete business flow: subscribe to the newsletter from the home page
 * footer and verify the confirmation message.
 */
public class NewsletterSubscriptionJourney implements Journey {

    @Override
    public String name() {
        return "Visitor subscribes to the newsletter from the footer";
    }

    @Override
    public void run(JourneyContext ctx) {
        String email = "newsletter." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        HomePage home = new HomePage(ctx.page());

        ctx.step("Open the home page");
        home.navigate();

        ctx.step("Subscribe with " + email);
        home.subscribeToNewsletter(email);

        ctx.step("Verify the success message");
        assertThat(home.subscriptionSuccessAlert())
                .containsText("You have been successfully subscribed!");
    }
}
