package com.promoquoter.pricing;

import java.math.BigDecimal;

public record PercentOffCategoryRule(PromotionRef promotion, ProductCategory category, BigDecimal percent)
        implements PricingRule {

    @Override
    public PricingStage stage() {
        return PricingStage.CATEGORY_DISCOUNT;
    }

    @Override
    public PricingContext apply(PricingContext context) {
        return context.mapLines(line -> line.category() == category
                ? line.withDiscount(line.total().percent(percent))
                : line);
    }
}
