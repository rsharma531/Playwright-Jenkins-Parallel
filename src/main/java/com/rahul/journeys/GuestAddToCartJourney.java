package com.rahul.journeys;

import com.rahul.framework.core.Journey;
import com.rahul.framework.core.JourneyContext;
import com.rahul.pages.CartPage;
import com.rahul.pages.ProductsPage;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;

/**
 * Complete business flow: a guest browses the catalogue, adds two products
 * to the cart and verifies the cart contents. Cart state lives in this
 * journey's own BrowserContext cookies, so parallel journeys never collide.
 */
public class GuestAddToCartJourney implements Journey {

    @Override
    public String name() {
        return "Guest adds two products to cart and verifies contents";
    }

    @Override
    public void run(JourneyContext ctx) {
        ProductsPage products = new ProductsPage(ctx.page());
        CartPage cart = new CartPage(ctx.page());

        ctx.step("Open the products catalogue");
        products.navigate();

        ctx.step("Add the first two products to the cart");
        String first = products.addProductToCart(0);
        String second = products.addProductToCart(1);
        ctx.step("Added: '" + first + "' and '" + second + "'");

        ctx.step("Open the cart and verify both products are present");
        products.goToCart();
        assertThat(cart.cartRows()).hasCount(2);

        List<String> inCart = cart.productNamesInCart();
        assertEquals(inCart, List.of(first, second),
                "Cart should contain exactly the two added products, in order");
    }
}
