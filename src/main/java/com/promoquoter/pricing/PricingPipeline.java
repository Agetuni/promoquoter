package com.promoquoter.pricing;

import java.util.List;

public class PricingPipeline {

    public Quote price(List<LineItem> lines, List<PricingRule> rules) {
        PricingContext context = new PricingContext(lines, List.of());
        List<PricingRule> chain = rules.stream()
                .sorted(PricingRule.EXECUTION_ORDER)
                .map(AuditingPricingRule::new)
                .map(PricingRule.class::cast)
                .toList();
        for (PricingRule rule : chain) {
            context = rule.apply(context);
        }
        return Quote.from(context);
    }
}
