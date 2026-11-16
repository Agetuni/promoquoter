package com.promoquoter.cart;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class InsufficientStockException extends ErrorResponseException {

    public record Shortage(UUID productId, String productName, int requested, int available) {
    }

    public InsufficientStockException(List<Shortage> shortages) {
        super(HttpStatus.CONFLICT);
        setTitle("Insufficient stock");
        setDetail("Not enough stock for: " + String.join(", ", shortages.stream().map(Shortage::productName).toList()));
        getBody().setProperty("shortages", shortages);
    }
}
