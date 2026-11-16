package com.promoquoter.cart;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartQueryService queries;
    private final CartCommandService commands;

    public CartController(CartQueryService queries, CartCommandService commands) {
        this.queries = queries;
        this.commands = commands;
    }

    @PostMapping("/quote")
    public QuoteResponse quote(@Valid @RequestBody CartRequest request) {
        return queries.quote(request);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirm(
            @Valid @RequestBody CartRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        var result = commands.confirm(request, idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.response());
    }
}
