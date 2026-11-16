package com.promoquoter.cart;

import com.promoquoter.pricing.PricingPipeline;
import com.promoquoter.pricing.Quote;
import com.promoquoter.product.Product;
import com.promoquoter.promotion.PromotionRepository;
import com.promoquoter.promotion.PromotionRuleFactory;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CartPricing {

    private final PromotionRepository promotions;
    private final PromotionRuleFactory ruleFactory;
    private final PricingPipeline pipeline = new PricingPipeline();

    public CartPricing(PromotionRepository promotions, PromotionRuleFactory ruleFactory) {
        this.promotions = promotions;
        this.ruleFactory = ruleFactory;
    }

    public Quote price(Map<UUID, Integer> quantities, Map<UUID, Product> products) {
        var lines = quantities.entrySet().stream()
                .map(entry -> products.get(entry.getKey()).toLineItem(entry.getValue()))
                .toList();
        var rules = promotions.findAll().stream().map(ruleFactory::create).toList();
        return pipeline.price(lines, rules);
    }
}
