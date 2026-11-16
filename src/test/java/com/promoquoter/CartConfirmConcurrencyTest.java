package com.promoquoter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CartConfirmConcurrencyTest extends ApiTest {

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Test
    void neverOversells() throws Exception {
        UUID lamp = createProduct("Lamp", "HOME", "30.00", 5);

        var responses = sendConcurrently(25, i -> confirmRequest(cart("REGULAR", lamp, 1), null));

        assertThat(responses).filteredOn(response -> response.statusCode() == 201).hasSize(5);
        assertThat(responses).filteredOn(response -> response.statusCode() == 409).hasSize(20);
        assertThat(stockOf(lamp)).isZero();
        assertThat(orders.count()).isEqualTo(5);
    }

    @Test
    void concurrentRetriesWithTheSameKeyReserveOnce() throws Exception {
        UUID lamp = createProduct("Lamp", "HOME", "30.00", 100);
        String cart = cart("REGULAR", lamp, 2);

        var responses = sendConcurrently(10, i -> confirmRequest(cart, "retry-key"));

        assertThat(responses).filteredOn(response -> response.statusCode() == 201).hasSize(1);
        assertThat(responses).filteredOn(response -> response.statusCode() == 200).hasSize(9);
        assertThat(responses.stream().map(response -> orderId(response.body())).collect(Collectors.toSet())).hasSize(1);
        assertThat(stockOf(lamp)).isEqualTo(98);
    }

    @Test
    void cartsWithProductsInOppositeOrderDoNotDeadlock() throws Exception {
        UUID a = createProduct("A", "TOYS", "1.00", 100);
        UUID b = createProduct("B", "TOYS", "1.00", 100);

        var responses = sendConcurrently(20, i -> confirmRequest(
                i % 2 == 0 ? cart("REGULAR", a, 1, b, 1) : cart("REGULAR", b, 1, a, 1), null));

        assertThat(responses).allMatch(response -> response.statusCode() == 201);
        assertThat(stockOf(a)).isEqualTo(80);
        assertThat(stockOf(b)).isEqualTo(80);
    }

    private String orderId(String body) {
        try {
            return json.readTree(body).get("orderId").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpRequest confirmRequest(String cart, String idempotencyKey) {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cart/confirm"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cart));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return request.build();
    }

    private List<HttpResponse<String>> sendConcurrently(int count, IntFunction<HttpRequest> request) throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<HttpResponse<String>>> futures = IntStream.range(0, count)
                    .mapToObj(i -> executor.submit(() -> {
                        start.await();
                        return http.send(request.apply(i), HttpResponse.BodyHandlers.ofString());
                    }))
                    .toList();
            start.countDown();
            List<HttpResponse<String>> responses = new ArrayList<>();
            for (var future : futures) {
                responses.add(future.get(60, TimeUnit.SECONDS));
            }
            return responses;
        }
    }
}
