package com.promoquoter.pricing;

import java.util.Comparator;

public interface PricingRule {

    Comparator<PricingRule> EXECUTION_ORDER = Comparator
            .comparing(PricingRule::stage)
            .thenComparingInt((PricingRule rule) -> rule.promotion().priority())
            .thenComparing(rule -> rule.promotion().id());

    PromotionRef promotion();

    PricingStage stage();

    PricingContext apply(PricingContext context);
}
