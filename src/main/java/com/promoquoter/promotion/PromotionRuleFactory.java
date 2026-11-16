package com.promoquoter.promotion;

import com.promoquoter.pricing.BuyXGetYRule;
import com.promoquoter.pricing.PercentOffCategoryRule;
import com.promoquoter.pricing.PricingRule;
import com.promoquoter.pricing.PromotionRef;
import org.springframework.stereotype.Component;

@Component
public class PromotionRuleFactory {

    public PricingRule create(Promotion promotion) {
        var ref = new PromotionRef(promotion.getId(), promotion.getName(), promotion.getPriority());
        return switch (promotion.getType()) {
            case PERCENT_OFF_CATEGORY -> new PercentOffCategoryRule(ref, promotion.getCategory(), promotion.getPercent());
            case BUY_X_GET_Y -> new BuyXGetYRule(ref, promotion.getProductId(), promotion.getBuyQuantity(),
                    promotion.getFreeQuantity());
        };
    }
}
