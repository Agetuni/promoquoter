package com.promoquoter.pricing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public record PricingContext(List<LineItem> lines, List<AppliedPromotion> appliedPromotions) {

    public PricingContext {
        lines = List.copyOf(lines);
        appliedPromotions = List.copyOf(appliedPromotions);
    }

    public Money total() {
        return lines.stream().map(LineItem::total).reduce(Money.ZERO, Money::plus);
    }

    public PricingContext mapLines(UnaryOperator<LineItem> change) {
        return new PricingContext(lines.stream().map(change).toList(), appliedPromotions);
    }

    public PricingContext withApplied(AppliedPromotion promotion) {
        var applied = new ArrayList<>(appliedPromotions);
        applied.add(promotion);
        return new PricingContext(lines, applied);
    }
}
