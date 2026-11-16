package com.promoquoter.promotion;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.promoquoter.pricing.ProductCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreatePromotionRequest.PercentOffCategory.class, name = "PERCENT_OFF_CATEGORY"),
        @JsonSubTypes.Type(value = CreatePromotionRequest.BuyXGetY.class, name = "BUY_X_GET_Y")
})
public sealed interface CreatePromotionRequest {

    Promotion toPromotion();

    record PercentOffCategory(
            @NotBlank String name,
            @PositiveOrZero Integer priority,
            @NotNull ProductCategory category,
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal percent)
            implements CreatePromotionRequest {

        @Override
        public Promotion toPromotion() {
            return Promotion.percentOffCategory(name, priority == null ? 0 : priority, category, percent);
        }
    }

    record BuyXGetY(
            @NotBlank String name,
            @PositiveOrZero Integer priority,
            @NotNull UUID productId,
            @NotNull @Positive Integer buyQuantity,
            @NotNull @Positive Integer freeQuantity)
            implements CreatePromotionRequest {

        @Override
        public Promotion toPromotion() {
            return Promotion.buyXGetY(name, priority == null ? 0 : priority, productId, buyQuantity, freeQuantity);
        }
    }
}
