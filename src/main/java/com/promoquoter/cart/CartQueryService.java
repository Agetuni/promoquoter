package com.promoquoter.cart;

import com.promoquoter.product.Product;
import com.promoquoter.product.ProductNotFoundException;
import com.promoquoter.product.ProductRepository;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartQueryService {

    private final ProductRepository products;
    private final CartPricing pricing;

    public CartQueryService(ProductRepository products, CartPricing pricing) {
        this.products = products;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public QuoteResponse quote(CartRequest request) {
        var quantities = request.quantities();
        Map<UUID, Product> found = products.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        quantities.keySet().stream()
                .filter(id -> !found.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new ProductNotFoundException(id);
                });
        return QuoteResponse.from(request.customerSegment(), pricing.price(quantities, found));
    }
}
