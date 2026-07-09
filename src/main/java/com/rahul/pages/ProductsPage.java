package com.rahul.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ProductsPage extends BasePage {

    public ProductsPage(Page page) {
        super(page);
    }

    public ProductsPage navigate() {
        open("/products");
        assertThat(page.locator(".features_items")).isVisible();
        return this;
    }

    public void searchFor(String term) {
        page.locator("#search_product").fill(term);
        page.locator("#submit_search").click();
    }

    public Locator resultsTitle() {
        return page.locator(".features_items h2.title");
    }

    public Locator productCards() {
        return page.locator(".features_items .product-image-wrapper");
    }

    /** Adds the nth listed product to the cart and dismisses the modal. */
    public String addProductToCart(int index) {
        Locator card = productCards().nth(index);
        card.scrollIntoViewIfNeeded();
        String productName = card.locator(".productinfo p").textContent().trim();
        card.locator(".productinfo a.add-to-cart").click();
        Locator modal = page.locator("#cartModal");
        assertThat(modal).isVisible();
        modal.locator("button.close-modal").click();
        assertThat(modal).isHidden();
        return productName;
    }

    public void goToCart() {
        page.locator("#header a[href='/view_cart']").click();
    }
}
