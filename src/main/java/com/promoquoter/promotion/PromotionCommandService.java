package com.promoquoter.promotion;

import com.promoquoter.product.ProductNotFoundException;
import com.promoquoter.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionCommandService {

    private final PromotionRepository promotions;
    private final ProductRepository products;

    public PromotionCommandService(PromotionRepository promotions, ProductRepository products) {
        this.promotions = promotions;
        this.products = products;
    }

    @Transactional
    public List<PromotionResponse> create(List<CreatePromotionRequest> requests) {
        for (CreatePromotionRequest request : requests) {
            if (request instanceof CreatePromotionRequest.BuyXGetY buyXGetY && !products.existsById(buyXGetY.productId())) {
                throw new ProductNotFoundException(buyXGetY.productId());
            }
        }
        return promotions.saveAll(requests.stream().map(CreatePromotionRequest::toPromotion).toList())
                .stream()
                .map(PromotionResponse::from)
                .toList();
    }
}
