package com.promoquoter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartConfirmApiTest extends ApiTest {

    private UUID headphones;
    private UUID lamp;

    @BeforeEach
    void catalog() throws Exception {
        headphones = createProduct("Headphones", "ELECTRONICS", "150.00", 20);
        lamp = createProduct("Lamp", "HOME", "30.00", 1);
        createPromotion("""
                {"type":"PERCENT_OFF_CATEGORY","name":"Electronics 10%","category":"ELECTRONICS","percent":10}""");
    }

    @Test
    void confirmReservesStockAndReturnsOrderIdAndFinalPrice() throws Exception {
        confirm(cart("REGULAR", headphones, 2, lamp, 1), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.finalPrice").value(300.0));

        assertThat(stockOf(headphones)).isEqualTo(18);
        assertThat(stockOf(lamp)).isZero();
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void insufficientStockIs409AndReservesNothing() throws Exception {
        confirm(cart("REGULAR", headphones, 2, lamp, 2), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.shortages[*].productName").value(containsInAnyOrder("Lamp")))
                .andExpect(jsonPath("$.shortages[0].requested").value(2))
                .andExpect(jsonPath("$.shortages[0].available").value(1));

        assertThat(stockOf(headphones)).isEqualTo(20);
        assertThat(orders.count()).isZero();
    }

    @Test
    void sameIdempotencyKeyDoesNotReserveTwice() throws Exception {
        String cart = cart("REGULAR", headphones, 2);

        String orderId = body(confirm(cart, "order-1").andExpect(status().isCreated())).get("orderId").asText();

        confirm(cart, "order-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.finalPrice").value(270.0));

        assertThat(stockOf(headphones)).isEqualTo(18);
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void sameKeyForADifferentCartIs422() throws Exception {
        confirm(cart("REGULAR", headphones, 1), "order-2").andExpect(status().isCreated());

        confirm(cart("REGULAR", headphones, 5), "order-2").andExpect(status().isUnprocessableEntity());

        assertThat(stockOf(headphones)).isEqualTo(19);
    }

    @Test
    void withoutKeyEveryConfirmCreatesAnOrder() throws Exception {
        confirm(cart("REGULAR", headphones, 1), null).andExpect(status().isCreated());
        confirm(cart("REGULAR", headphones, 1), null).andExpect(status().isCreated());

        assertThat(orders.count()).isEqualTo(2);
        assertThat(stockOf(headphones)).isEqualTo(18);
    }

    @Test
    void unknownProductIs404() throws Exception {
        confirm(cart("REGULAR", headphones, 1, UUID.randomUUID(), 1), null).andExpect(status().isNotFound());

        assertThat(stockOf(headphones)).isEqualTo(20);
    }
}
