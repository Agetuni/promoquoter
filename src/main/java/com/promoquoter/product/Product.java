package com.promoquoter.product;

import com.promoquoter.pricing.LineItem;
import com.promoquoter.pricing.Money;
import com.promoquoter.pricing.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Version
    private Long version;

    protected Product() {
    }

    public Product(String name, ProductCategory category, BigDecimal price, int stock) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public void reserve(int quantity) {
        if (quantity > stock) {
            throw new IllegalStateException("Insufficient stock for product " + id);
        }
        stock -= quantity;
    }

    public LineItem toLineItem(int quantity) {
        return LineItem.of(id, name, category, Money.of(price), quantity);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }
}
