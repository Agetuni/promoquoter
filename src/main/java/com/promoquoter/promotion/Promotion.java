package com.promoquoter.promotion;

import com.promoquoter.pricing.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Promotion {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Column(precision = 5, scale = 2)
    private BigDecimal percent;

    private UUID productId;

    private Integer buyQuantity;

    private Integer freeQuantity;

    protected Promotion() {
    }

    private Promotion(String name, PromotionType type, int priority) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.type = type;
        this.priority = priority;
    }

    public static Promotion percentOffCategory(String name, int priority, ProductCategory category, BigDecimal percent) {
        var promotion = new Promotion(name, PromotionType.PERCENT_OFF_CATEGORY, priority);
        promotion.category = category;
        promotion.percent = percent;
        return promotion;
    }

    public static Promotion buyXGetY(String name, int priority, UUID productId, int buyQuantity, int freeQuantity) {
        var promotion = new Promotion(name, PromotionType.BUY_X_GET_Y, priority);
        promotion.productId = productId;
        promotion.buyQuantity = buyQuantity;
        promotion.freeQuantity = freeQuantity;
        return promotion;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PromotionType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public UUID getProductId() {
        return productId;
    }

    public Integer getBuyQuantity() {
        return buyQuantity;
    }

    public Integer getFreeQuantity() {
        return freeQuantity;
    }
}
