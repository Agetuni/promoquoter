package com.promoquoter.product;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCommandService {

    private final ProductRepository products;

    public ProductCommandService(ProductRepository products) {
        this.products = products;
    }

    @Transactional
    public List<ProductResponse> create(List<CreateProductRequest> requests) {
        return products.saveAll(requests.stream().map(CreateProductRequest::toProduct).toList())
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
