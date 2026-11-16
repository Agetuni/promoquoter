package com.promoquoter.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmResponse(UUID orderId, BigDecimal finalPrice) {

    static ConfirmResponse from(Order order) {
        return new ConfirmResponse(order.getId(), order.getFinalPrice());
    }
}
