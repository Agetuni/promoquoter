package com.promoquoter.pricing;

import java.util.UUID;

public record LineItem(
        UUID productId,
        String productName,
        ProductCategory category,
        Money unitPrice,
        int quantity,
        Money discount) {

    public LineItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
    }

    public static LineItem of(UUID productId, String productName, ProductCategory category, Money unitPrice, int quantity) {
        return new LineItem(productId, productName, category, unitPrice, quantity, Money.ZERO);
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }

    public Money total() {
        return subtotal().minus(discount);
    }

    public LineItem withDiscount(Money amount) {
        return new LineItem(productId, productName, category, unitPrice, quantity, discount.plus(amount.min(total())));
    }
}
