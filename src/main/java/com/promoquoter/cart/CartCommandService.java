package com.promoquoter.cart;

import com.promoquoter.pricing.CustomerSegment;
import com.promoquoter.product.Product;
import com.promoquoter.product.ProductNotFoundException;
import com.promoquoter.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartCommandService {

    public record Result(ConfirmResponse response, boolean replayed) {
    }

    private final ProductRepository products;
    private final OrderRepository orders;
    private final CartPricing pricing;

    public CartCommandService(ProductRepository products, OrderRepository orders, CartPricing pricing) {
        this.products = products;
        this.orders = orders;
        this.pricing = pricing;
    }

    @Transactional
    public Result confirm(CartRequest request, String idempotencyKey) {
        var quantities = request.quantities();
        String fingerprint = fingerprint(request.customerSegment(), quantities);
        Map<UUID, Product> locked = lockInIdOrder(quantities);

        Optional<Order> existing = idempotencyKey == null ? Optional.empty() : orders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw new IdempotencyKeyReusedException(idempotencyKey);
            }
            return new Result(ConfirmResponse.from(existing.get()), true);
        }

        var shortages = quantities.entrySet().stream()
                .filter(entry -> locked.get(entry.getKey()).getStock() < entry.getValue())
                .map(entry -> {
                    Product product = locked.get(entry.getKey());
                    return new InsufficientStockException.Shortage(
                            product.getId(), product.getName(), entry.getValue(), product.getStock());
                })
                .toList();
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }

        var quote = pricing.price(quantities, locked);
        quantities.forEach((productId, quantity) -> locked.get(productId).reserve(quantity));
        Order order = orders.save(new Order(idempotencyKey, fingerprint, request.customerSegment(), quote.total().amount()));
        return new Result(ConfirmResponse.from(order), false);
    }

    private Map<UUID, Product> lockInIdOrder(Map<UUID, Integer> quantities) {
        Map<UUID, Product> locked = new HashMap<>();
        quantities.keySet().stream().sorted().forEach(id -> locked.put(id,
                products.findByIdForUpdate(id).orElseThrow(() -> new ProductNotFoundException(id))));
        return locked;
    }

    private static String fingerprint(CustomerSegment segment, Map<UUID, Integer> quantities) {
        String canonical = segment + "|" + quantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "x" + entry.getValue())
                .collect(Collectors.joining(","));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
