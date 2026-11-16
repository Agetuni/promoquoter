package com.promoquoter.cart;

import com.promoquoter.pricing.CustomerSegment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(unique = true)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerSegment customerSegment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    protected Order() {
    }

    Order(String idempotencyKey, String requestFingerprint, CustomerSegment customerSegment, BigDecimal finalPrice) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.customerSegment = customerSegment;
        this.finalPrice = finalPrice;
    }

    public UUID getId() {
        return id;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }
}
