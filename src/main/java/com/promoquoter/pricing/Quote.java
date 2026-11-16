package com.promoquoter.pricing;

import java.util.List;

public record Quote(List<LineItem> lines, List<AppliedPromotion> appliedPromotions, Money subtotal, Money discount, Money total) {

    public static Quote from(PricingContext context) {
        Money subtotal = context.lines().stream().map(LineItem::subtotal).reduce(Money.ZERO, Money::plus);
        Money total = context.total();
        return new Quote(context.lines(), context.appliedPromotions(), subtotal, subtotal.minus(total), total);
    }
}
