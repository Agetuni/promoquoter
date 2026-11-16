package com.promoquoter.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.promoquoter.pricing.ProductCategory;
import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromotionResponse(
        UUID id,
        String name,
        PromotionType type,
        int priority,
        ProductCategory category,
        BigDecimal percent,
        UUID productId,
        Integer buyQuantity,
        Integer freeQuantity) {

    static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getName(), promotion.getType(),
                promotion.getPriority(), promotion.getCategory(), promotion.getPercent(), promotion.getProductId(),
                promotion.getBuyQuantity(), promotion.getFreeQuantity());
    }
}
