package com.promoquoter.product;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ProductNotFoundException extends ErrorResponseException {

    public ProductNotFoundException(UUID productId) {
        super(HttpStatus.NOT_FOUND);
        setTitle("Product not found");
        setDetail("Product " + productId + " does not exist");
        getBody().setProperty("productId", productId);
    }
}
