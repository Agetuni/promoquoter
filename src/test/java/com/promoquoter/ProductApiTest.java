package com.promoquoter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class ProductApiTest extends ApiTest {

    @Test
    void createsSeveralProducts() throws Exception {
        postJson("/products", """
                [{"name":"Kettle","category":"HOME","price":49.9,"stock":12},
                 {"name":"Novel","category":"BOOKS","price":15,"stock":0}]""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].price").value(49.9))
                .andExpect(jsonPath("$[1].stock").value(0));

        assertThat(products.count()).isEqualTo(2);
    }

    @Test
    void createsASingleProduct() throws Exception {
        postJson("/products", """
                {"name":"Ball","category":"SPORTS","price":9.99,"stock":3}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ball"));
    }

    @Test
    void rejectsInvalidProducts() throws Exception {
        postJson("/products", """
                [{"name":"Valid","category":"BOOKS","price":5,"stock":1},
                 {"name":"","price":-5,"stock":-1}]""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItems("[1].name", "[1].category", "[1].price", "[1].stock")));

        assertThat(products.count()).isZero();
    }

    @Test
    void rejectsAnEmptyList() throws Exception {
        postJson("/products", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("products"));
    }
}
