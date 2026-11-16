package com.promoquoter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartQuoteApiTest extends ApiTest {

    private UUID headphones;
    private UUID charger;
    private UUID tshirt;

    @BeforeEach
    void catalog() throws Exception {
        headphones = createProduct("Headphones", "ELECTRONICS", "150.00", 20);
        charger = createProduct("Charger", "ELECTRONICS", "25.00", 100);
        tshirt = createProduct("T-Shirt", "CLOTHING", "20.00", 50);
        createPromotion("""
                [{"type":"PERCENT_OFF_CATEGORY","name":"Electronics 10%%","category":"ELECTRONICS","percent":10},
                 {"type":"BUY_X_GET_Y","name":"T-Shirt 3 for 2","priority":1,"productId":"%s","buyQuantity":2,"freeQuantity":1},
                 {"type":"BUY_X_GET_Y","name":"Charger 4 for 3","priority":2,"productId":"%s","buyQuantity":3,"freeQuantity":1}]"""
                .formatted(tshirt, charger));
    }

    @Test
    void itemisedQuoteWithPromotionsInOrder() throws Exception {
        postJson("/cart/quote", cart("REGULAR", headphones, 2, charger, 4, tshirt, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerSegment").value("REGULAR"))
                .andExpect(jsonPath("$.lines[*].productName").value(contains("Headphones", "Charger", "T-Shirt")))
                .andExpect(jsonPath("$.lines[*].subtotal").value(contains(300.0, 100.0, 60.0)))
                .andExpect(jsonPath("$.lines[*].discount").value(contains(30.0, 32.5, 20.0)))
                .andExpect(jsonPath("$.lines[*].total").value(contains(270.0, 67.5, 40.0)))
                .andExpect(jsonPath("$.appliedPromotions[*].name")
                        .value(contains("Electronics 10%", "T-Shirt 3 for 2", "Charger 4 for 3")))
                .andExpect(jsonPath("$.appliedPromotions[*].discount").value(contains(40.0, 20.0, 22.5)))
                .andExpect(jsonPath("$.subtotal").value(460.0))
                .andExpect(jsonPath("$.discount").value(82.5))
                .andExpect(jsonPath("$.total").value(377.5));
    }

    @Test
    void repeatedProductsAreMerged() throws Exception {
        postJson("/cart/quote", cart("REGULAR", tshirt, 1, tshirt, 2))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(40.0));
    }

    @Test
    void quotingDoesNotReserveStock() throws Exception {
        postJson("/cart/quote", cart("REGULAR", headphones, 5)).andExpect(status().isOk());

        assertThat(stockOf(headphones)).isEqualTo(20);
    }

    @Test
    void unknownProductIs404() throws Exception {
        UUID unknown = UUID.randomUUID();

        postJson("/cart/quote", cart("REGULAR", headphones, 1, unknown, 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.productId").value(unknown.toString()));
    }

    @Test
    void invalidCartIs400WithFieldErrors() throws Exception {
        postJson("/cart/quote", """
                {"items":[{"productId":"%s","qty":0},{"qty":1}]}""".formatted(tshirt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(contains("customerSegment", "items[0].qty", "items[1].productId")));
    }

    @Test
    void emptyCartIs400() throws Exception {
        postJson("/cart/quote", """
                {"items":[],"customerSegment":"REGULAR"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        postJson("/cart/quote", "{\"items\": [").andExpect(status().isBadRequest());
    }
}
