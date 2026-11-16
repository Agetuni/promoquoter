package com.promoquoter.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingPipelineTest {

    private final PricingPipeline pipeline = new PricingPipeline();

    private static LineItem line(String name, ProductCategory category, String price, int quantity) {
        return LineItem.of(UUID.randomUUID(), name, category, Money.of(price), quantity);
    }

    private static PromotionRef promotion(String name, int priority) {
        return new PromotionRef(UUID.randomUUID(), name, priority);
    }

    private static PercentOffCategoryRule percentOff(String name, int priority, ProductCategory category, String percent) {
        return new PercentOffCategoryRule(promotion(name, priority), category, new BigDecimal(percent));
    }

    private static BuyXGetYRule buyXGetY(String name, int priority, LineItem line, int buy, int free) {
        return new BuyXGetYRule(promotion(name, priority), line.productId(), buy, free);
    }

    @Test
    void referenceCart() {
        LineItem headphones = line("Headphones", ProductCategory.ELECTRONICS, "150.00", 2);
        LineItem charger = line("Charger", ProductCategory.ELECTRONICS, "25.00", 4);
        LineItem tshirt = line("T-Shirt", ProductCategory.CLOTHING, "20.00", 3);

        Quote quote = pipeline.price(List.of(headphones, charger, tshirt), List.of(
                buyXGetY("Charger 4 for 3", 2, charger, 3, 1),
                buyXGetY("T-Shirt 3 for 2", 1, tshirt, 2, 1),
                percentOff("Electronics 10%", 0, ProductCategory.ELECTRONICS, "10")));

        assertThat(quote.lines()).extracting(LineItem::total)
                .containsExactly(Money.of("270.00"), Money.of("67.50"), Money.of("40.00"));
        assertThat(quote.appliedPromotions()).extracting(AppliedPromotion::name, AppliedPromotion::discount)
                .containsExactly(
                        tuple("Electronics 10%", Money.of("40.00")),
                        tuple("T-Shirt 3 for 2", Money.of("20.00")),
                        tuple("Charger 4 for 3", Money.of("22.50")));
        assertThat(quote.subtotal()).isEqualTo(Money.of("460.00"));
        assertThat(quote.discount()).isEqualTo(Money.of("82.50"));
        assertThat(quote.total()).isEqualTo(Money.of("377.50"));
    }

    @ParameterizedTest
    @CsvSource({"1, 1, 1, 0", "1, 1, 2, 1", "1, 1, 3, 1", "2, 1, 3, 1", "2, 1, 5, 1", "2, 1, 6, 2", "1, 2, 3, 2"})
    void buyXGetYMakesYFreeForEveryCompleteBundle(int buy, int free, int quantity, int expectedFree) {
        LineItem shirt = line("Shirt", ProductCategory.CLOTHING, "10.00", quantity);

        Quote quote = pipeline.price(List.of(shirt), List.of(buyXGetY("bxgy", 0, shirt, buy, free)));

        assertThat(quote.discount()).isEqualTo(Money.of("10.00").times(expectedFree));
    }

    @Test
    void percentOffOnlyTouchesTheTargetCategory() {
        Quote quote = pipeline.price(
                List.of(line("Cable", ProductCategory.ELECTRONICS, "9.99", 3), line("Book", ProductCategory.BOOKS, "5.00", 1)),
                List.of(percentOff("10%", 0, ProductCategory.ELECTRONICS, "10")));

        assertThat(quote.lines()).extracting(LineItem::discount).containsExactly(Money.of("3.00"), Money.ZERO);
    }

    @Test
    void freeItemsAreValuedAfterEarlierDiscountsWithHalfUpRounding() {
        LineItem toy = line("Toy", ProductCategory.TOYS, "3.33", 3);

        Quote quote = pipeline.price(List.of(toy), List.of(
                buyXGetY("b2g1", 0, toy, 2, 1),
                percentOff("10%", 0, ProductCategory.TOYS, "10")));

        assertThat(quote.total()).isEqualTo(Money.of("5.99"));
    }

    @Test
    void totalsNeverGoNegative() {
        LineItem shirt = line("Shirt", ProductCategory.CLOTHING, "20.00", 2);

        Quote quote = pipeline.price(List.of(shirt), List.of(
                percentOff("all", 0, ProductCategory.CLOTHING, "100"),
                percentOff("all again", 1, ProductCategory.CLOTHING, "100"),
                buyXGetY("b1g1", 0, shirt, 1, 1)));

        assertThat(quote.total()).isEqualTo(Money.ZERO);
        assertThat(quote.discount()).isEqualTo(Money.of("40.00"));
        assertThat(quote.appliedPromotions()).extracting(AppliedPromotion::name).containsExactly("all");
    }

    @Test
    void sameStageRunsByPriority() {
        LineItem book = line("Book", ProductCategory.BOOKS, "100.00", 1);

        Quote quote = pipeline.price(List.of(book), List.of(
                percentOff("second", 5, ProductCategory.BOOKS, "50"),
                percentOff("first", 1, ProductCategory.BOOKS, "10")));

        assertThat(quote.appliedPromotions()).extracting(AppliedPromotion::name, AppliedPromotion::discount)
                .containsExactly(
                        tuple("first", Money.of("10.00")),
                        tuple("second", Money.of("45.00")));
    }

    @Test
    void auditingDecoratorDelegates() {
        var rule = percentOff("p", 3, ProductCategory.BOOKS, "5");
        var audited = new AuditingPricingRule(rule);

        assertThat(audited.promotion()).isEqualTo(rule.promotion());
        assertThat(audited.stage()).isEqualTo(PricingStage.CATEGORY_DISCOUNT);
    }

    @Test
    void lineQuantityMustBePositive() {
        assertThatThrownBy(() -> line("x", ProductCategory.TOYS, "1.00", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
