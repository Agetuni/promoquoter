package com.promoquoter.pricing;

import java.util.UUID;

public record BuyXGetYRule(PromotionRef promotion, UUID productId, int buyQuantity, int freeQuantity)
        implements PricingRule {

    @Override
    public PricingStage stage() {
        return PricingStage.BUY_X_GET_Y;
    }

    @Override
    public PricingContext apply(PricingContext context) {
        return context.mapLines(line -> line.productId().equals(productId) ? applyTo(line) : line);
    }

    private LineItem applyTo(LineItem line) {
        int freeUnits = line.quantity() / (buyQuantity + freeQuantity) * freeQuantity;
        if (freeUnits == 0) {
            return line;
        }
        return line.withDiscount(line.total().fraction(freeUnits, line.quantity()));
    }
}
