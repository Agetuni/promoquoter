package com.promoquoter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promoquoter.cart.OrderRepository;
import com.promoquoter.product.ProductRepository;
import com.promoquoter.promotion.PromotionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected ProductRepository products;

    @Autowired
    protected PromotionRepository promotions;

    @Autowired
    protected OrderRepository orders;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void cleanDatabase() {
        orders.deleteAll();
        promotions.deleteAll();
        products.deleteAll();
    }

    protected ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions confirm(String cart, String idempotencyKey) throws Exception {
        var request = post("/cart/confirm").contentType(MediaType.APPLICATION_JSON).content(cart);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mvc.perform(request);
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected UUID createProduct(String name, String category, String price, int stock) throws Exception {
        var result = postJson("/products", """
                {"name":"%s","category":"%s","price":%s,"stock":%d}""".formatted(name, category, price, stock))
                .andExpect(status().isCreated());
        return UUID.fromString(body(result).get(0).get("id").asText());
    }

    protected void createPromotion(String promotion) throws Exception {
        postJson("/promotions", promotion).andExpect(status().isCreated());
    }

    protected int stockOf(UUID productId) {
        return products.findById(productId).orElseThrow().getStock();
    }

    protected static String cart(String segment, Object... productIdAndQuantity) {
        var items = new StringBuilder();
        for (int i = 0; i < productIdAndQuantity.length; i += 2) {
            items.append(items.isEmpty() ? "" : ",")
                    .append("{\"productId\":\"%s\",\"qty\":%s}".formatted(productIdAndQuantity[i], productIdAndQuantity[i + 1]));
        }
        return "{\"items\":[%s],\"customerSegment\":\"%s\"}".formatted(items, segment);
    }
}
