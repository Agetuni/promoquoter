# PromoQuoter

A Spring Boot REST service that prices a cart using pluggable promotion rules and reserves inventory when the cart is confirmed.

Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Bean Validation, H2 (in-memory).

## How to run

Requirements: JDK 17 or newer (21 recommended) and Maven 3.9+.

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

```bash
mvn verify
```

This runs the unit and integration tests and checks coverage (minimum 90% of lines). The coverage report is written to `target/site/jacoco/index.html`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/products` | Create one product (JSON object) or several (JSON array) |
| `POST` | `/promotions` | Create one promotion or several |
| `POST` | `/cart/quote` | Itemised price breakdown with the applied promotions, in order |
| `POST` | `/cart/confirm` | Validate stock, reserve it atomically and return `orderId` and `finalPrice`. Optional `Idempotency-Key` header |

## Sample requests

**1. Create products.** Copy the `id` values from the response; the next steps refer to them as `HEADPHONES_ID`, `CHARGER_ID` and `TSHIRT_ID`.

```bash
curl -X POST http://localhost:8080/products -H "Content-Type: application/json" -d '[
  {"name": "Headphones", "category": "ELECTRONICS", "price": 150.00, "stock": 20},
  {"name": "Charger", "category": "ELECTRONICS", "price": 25.00, "stock": 100},
  {"name": "T-Shirt", "category": "CLOTHING", "price": 20.00, "stock": 50}
]'
```

**2. Create promotions.**

```bash
curl -X POST http://localhost:8080/promotions -H "Content-Type: application/json" -d '[
  {"type": "PERCENT_OFF_CATEGORY", "name": "Electronics 10%", "category": "ELECTRONICS", "percent": 10},
  {"type": "BUY_X_GET_Y", "name": "T-Shirt 3 for 2", "priority": 1, "productId": "TSHIRT_ID", "buyQuantity": 2, "freeQuantity": 1},
  {"type": "BUY_X_GET_Y", "name": "Charger 4 for 3", "priority": 2, "productId": "CHARGER_ID", "buyQuantity": 3, "freeQuantity": 1}
]'
```

**3. Quote the cart.**

```bash
curl -X POST http://localhost:8080/cart/quote -H "Content-Type: application/json" -d '{
  "items": [
    {"productId": "HEADPHONES_ID", "qty": 2},
    {"productId": "CHARGER_ID", "qty": 4},
    {"productId": "TSHIRT_ID", "qty": 3}
  ],
  "customerSegment": "REGULAR"
}'
```

```json
{
  "customerSegment": "REGULAR",
  "lines": [
    {"productName": "Headphones", "unitPrice": 150.00, "quantity": 2, "subtotal": 300.00, "discount": 30.00, "total": 270.00},
    {"productName": "Charger", "unitPrice": 25.00, "quantity": 4, "subtotal": 100.00, "discount": 32.50, "total": 67.50},
    {"productName": "T-Shirt", "unitPrice": 20.00, "quantity": 3, "subtotal": 60.00, "discount": 20.00, "total": 40.00}
  ],
  "appliedPromotions": [
    {"name": "Electronics 10%", "discount": 40.00},
    {"name": "T-Shirt 3 for 2", "discount": 20.00},
    {"name": "Charger 4 for 3", "discount": 22.50}
  ],
  "subtotal": 460.00,
  "discount": 82.50,
  "total": 377.50
}
```

**4. Confirm the cart.** Send the same request twice with the same key: the second call returns the same order and does not reserve stock again.

```bash
curl -i -X POST http://localhost:8080/cart/confirm -H "Content-Type: application/json" -H "Idempotency-Key: order-001" -d '{
  "items": [
    {"productId": "HEADPHONES_ID", "qty": 2},
    {"productId": "CHARGER_ID", "qty": 4},
    {"productId": "TSHIRT_ID", "qty": 3}
  ],
  "customerSegment": "REGULAR"
}'
```

```json
{"orderId": "2f0c5c1e-6b8a-4a51-9d3e-7f3a1c2b9e40", "finalPrice": 377.50}
```

## Design

### Lightweight CQRS

Each feature separates writes (commands) from reads (queries). There is no event bus and no separate read database: the split is at the service level.

| Side | Class | Operation |
|---|---|---|
| Command | `ProductCommandService` | Create products |
| Command | `PromotionCommandService` | Create promotions |
| Command | `CartCommandService` | Confirm a cart: lock rows, check stock, reserve, save the order |
| Query | `CartQueryService` | Quote a cart: read-only, with no side effects |

Both cart services share `CartPricing`, which loads the promotions and runs the pricing pipeline.

### Rules engine

| Pattern | Class | Role |
|---|---|---|
| Strategy | `PricingRule`, `PercentOffCategoryRule`, `BuyXGetYRule` | Each rule encapsulates one pricing algorithm |
| Pipeline | `PricingPipeline` | Sorts the rules and applies them one after another |
| Factory | `PromotionRuleFactory` | Builds a rule from a saved promotion |
| Decorator | `AuditingPricingRule` | Wraps every rule and records each applied promotion and its discount (the audit trail) |

The `pricing` package has no Spring or JPA dependencies.

To add a promotion type:
1. Write a new `PricingRule`.
2. Add a request record and a factory case.

`PricingPipeline` does not change.

### Concurrency and idempotency

- `POST /cart/confirm` runs in one transaction.
- It locks each product row with a pessimistic write lock (`SELECT ... FOR UPDATE`), always in ascending id order, so concurrent checkouts cannot oversell and cannot deadlock.
- Stock is checked for every line before anything changes. If any line is short, the response is `409 Conflict` with the list of shortages, and nothing is reserved.
- `Product` also has a `@Version` field for optimistic locking.
- **Idempotency:**
  - The order stores the `Idempotency-Key` (unique column) and a SHA-256 fingerprint of the cart.
  - A repeated key with the same cart returns the stored order with `200 OK`, so stock is not reserved twice.
  - A repeated key with a different cart returns `422 Unprocessable Entity`.
  - The key is checked after the row locks are taken, so concurrent retries with the same key are also safe.

## Assumptions

- **Money:**
  - Single currency.
  - All amounts are rounded HALF_UP to 2 decimals.
  - Prices can never go negative: each discount is capped at what is still payable on its line.
- **Order of promotions:**
  - Category discounts (`PERCENT_OFF_CATEGORY`) are applied before `BUY_X_GET_Y`.
  - Within the same type, lower `priority` runs first (default 0); the promotion id breaks ties.
- **Stacking:** all matching promotions apply. Each one discounts the line's current total, so discounts compound and a free item is valued at its already-discounted price.
- **Buy X get Y:**
  - For every complete group of X + Y units, Y units are free.
  - Buy 1 get 1 on a single unit gives no discount.
  - Buy 2 get 1 on 6 units makes 2 units free.
- **Cart lines:** the same product listed twice in a cart is merged into one line.
- **Promotions:** a `BUY_X_GET_Y` promotion must reference an existing product (`404` otherwise).
- **Customer segment:** `customerSegment` is required and is stored on the order. No promotion depends on it.
- **Storage:** data lives in an in-memory H2 database and is lost on restart.

## Errors

All errors are returned as RFC 9457 problem details (`application/problem+json`).

| Status | When |
|---|---|
| 400 | Validation failure (the `errors` list gives each field and message), malformed JSON, unknown promotion type |
| 404 | Unknown product |
| 409 | Insufficient stock (`shortages` lists each product with requested and available quantity), or a conflicting concurrent request |
| 422 | `Idempotency-Key` reused for a different cart |

```json
{
  "title": "Insufficient stock",
  "status": 409,
  "detail": "Not enough stock for: Lamp",
  "instance": "/cart/confirm",
  "shortages": [{"productId": "...", "productName": "Lamp", "requested": 2, "available": 1}]
}
```

## Tests

| Test class | Type | Covers |
|---|---|---|
| `MoneyTest` | Unit | HALF_UP rounding, no negative amounts, arithmetic |
| `PricingPipelineTest` | Unit | Both rules, rule order, compounding, never-negative totals, audit trail |
| `ProductApiTest`, `PromotionApiTest` | Integration | Create endpoints and validation |
| `CartQuoteApiTest` | Integration | Itemised quote, applied promotions in order, merging, errors |
| `CartConfirmApiTest` | Integration | Reservation, 409 without partial reservation, idempotency, 422 |
| `CartConfirmConcurrencyTest` | Integration | Concurrent HTTP requests: no overselling, one reservation per key, no deadlock |

There are 50 tests in total. Line coverage is 97%.
