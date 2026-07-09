package com.rahul.journeys;

import com.rahul.framework.core.Journey;
import com.rahul.framework.core.JourneyContext;
import com.rahul.pages.ProductsPage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertTrue;

/**
 * Complete business flow: search the catalogue and verify relevant results.
 */
public class ProductSearchJourney implements Journey {

    private static final String SEARCH_TERM = "dress";

    @Override
    public String name() {
        return "Guest searches the catalogue for '" + SEARCH_TERM + "'";
    }

    @Override
    public void run(JourneyContext ctx) {
        ProductsPage products = new ProductsPage(ctx.page());

        ctx.step("Open the products catalogue");
        products.navigate();

        ctx.step("Search for '" + SEARCH_TERM + "'");
        products.searchFor(SEARCH_TERM);
        assertThat(products.resultsTitle()).hasText("Searched Products");

        int count = products.productCards().count();
        ctx.step("Search returned " + count + " products");
        assertTrue(count > 0, "Expected at least one product for '" + SEARCH_TERM + "'");
    }
}
