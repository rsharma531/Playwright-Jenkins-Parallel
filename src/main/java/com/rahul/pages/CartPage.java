package com.rahul.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class CartPage extends BasePage {

    public CartPage(Page page) {
        super(page);
    }

    public Locator cartRows() {
        return page.locator("#cart_info_table tbody tr");
    }

    public List<String> productNamesInCart() {
        return cartRows().locator(".cart_description h4 a").allTextContents()
                .stream().map(String::trim).toList();
    }
}
