package com.promoquoter.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({"10, 10.00", "1.005, 1.01", "1.004, 1.00", "2.345, 2.35"})
    void roundsHalfUpToCents(String raw, String expected) {
        assertThat(Money.of(raw).amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void cannotBeNegative() {
        assertThatThrownBy(() -> Money.of("-0.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of("1.00").minus(Money.of("1.01"))).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"213.75, 10, 21.38", "0.05, 50, 0.03", "100.00, 100, 100.00"})
    void percent(String amount, String percent, String expected) {
        assertThat(Money.of(amount).percent(new BigDecimal(percent))).isEqualTo(Money.of(expected));
    }

    @ParameterizedTest
    @CsvSource({"100.00, 1, 3, 33.33", "200.00, 2, 3, 133.33", "8.99, 1, 3, 3.00"})
    void fraction(String amount, int numerator, int denominator, String expected) {
        assertThat(Money.of(amount).fraction(numerator, denominator)).isEqualTo(Money.of(expected));
    }

    @Test
    void arithmetic() {
        assertThat(Money.of("2.50").plus(Money.of("0.75"))).isEqualTo(Money.of("3.25"));
        assertThat(Money.of("19.99").times(3)).isEqualTo(Money.of("59.97"));
        assertThat(Money.of("1").min(Money.of("2"))).isEqualTo(Money.of("1"));
        assertThat(Money.of("2").min(Money.of("1"))).isEqualTo(Money.of("1"));
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.of(new BigDecimal("7.5"))).hasToString("7.50");
    }
}
