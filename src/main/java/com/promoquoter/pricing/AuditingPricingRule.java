package com.promoquoter.pricing;

public record AuditingPricingRule(PricingRule delegate) implements PricingRule {

    @Override
    public PromotionRef promotion() {
        return delegate.promotion();
    }

    @Override
    public PricingStage stage() {
        return delegate.stage();
    }

    @Override
    public PricingContext apply(PricingContext context) {
        PricingContext next = delegate.apply(context);
        Money discount = context.total().minus(next.total());
        if (discount.isZero()) {
            return next;
        }
        return next.withApplied(new AppliedPromotion(promotion().id(), promotion().name(), discount));
    }
}
