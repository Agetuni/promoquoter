package com.promoquoter.cart;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class IdempotencyKeyReusedException extends ErrorResponseException {

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super(HttpStatus.UNPROCESSABLE_ENTITY);
        setTitle("Idempotency key reused");
        setDetail("Idempotency-Key '" + idempotencyKey + "' was already used for a different cart");
    }
}
