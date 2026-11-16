package com.promoquoter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromotionApiTest extends ApiTest {

    @Test
    void createsBothPromotionTypes() throws Exception {
        UUID shirt = createProduct("Shirt", "CLOTHING", "20.00", 5);

        postJson("/promotions", """
                [{"type":"PERCENT_OFF_CATEGORY","name":"Books 10%%","category":"BOOKS","percent":10},
                 {"type":"BUY_X_GET_Y","name":"Shirt 3 for 2","priority":1,"productId":"%s","buyQuantity":2,"freeQuantity":1}]"""
                .formatted(shirt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].type").value("PERCENT_OFF_CATEGORY"))
                .andExpect(jsonPath("$[0].priority").value(0))
                .andExpect(jsonPath("$[0].percent").value(10))
                .andExpect(jsonPath("$[0].productId").doesNotExist())
                .andExpect(jsonPath("$[1].type").value("BUY_X_GET_Y"))
                .andExpect(jsonPath("$[1].productId").value(shirt.toString()))
                .andExpect(jsonPath("$[1].buyQuantity").value(2));

        assertThat(promotions.count()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        postJson("/promotions", """
                [{"type":"PERCENT_OFF_CATEGORY","name":"Too much","category":"BOOKS","percent":150},
                 {"type":"BUY_X_GET_Y","name":"","productId":"%s","buyQuantity":0}]""".formatted(UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.contains(
                        "[0].percent", "[1].buyQuantity", "[1].freeQuantity", "[1].name")));
    }

    @Test
    void rejectsUnknownType() throws Exception {
        postJson("/promotions", """
                {"type":"MYSTERY","name":"x"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBuyXGetYForUnknownProduct() throws Exception {
        UUID unknown = UUID.randomUUID();

        postJson("/promotions", """
                {"type":"BUY_X_GET_Y","name":"Ghost","productId":"%s","buyQuantity":1,"freeQuantity":1}""".formatted(unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.productId").value(unknown.toString()));

        assertThat(promotions.count()).isZero();
    }
}
