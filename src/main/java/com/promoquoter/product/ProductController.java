package com.promoquoter.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductCommandService commands;

    public ProductController(ProductCommandService commands) {
        this.commands = commands;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProductResponse> create(@RequestBody @NotEmpty List<@Valid @NotNull CreateProductRequest> products) {
        return commands.create(products);
    }
}
