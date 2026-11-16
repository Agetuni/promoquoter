package com.promoquoter.cart;

import com.promoquoter.pricing.CustomerSegment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CartRequest(
        @NotEmpty List<@Valid @NotNull Item> items,
        @NotNull CustomerSegment customerSegment) {

    public record Item(@NotNull UUID productId, @NotNull @Positive Integer qty) {
    }

    Map<UUID, Integer> quantities() {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        items.forEach(item -> quantities.merge(item.productId(), item.qty(), Integer::sum));
        return quantities;
    }
}
