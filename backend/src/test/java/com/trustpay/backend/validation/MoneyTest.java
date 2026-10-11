package com.trustpay.backend.validation;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    @Test
    void rejectsMissingNegativeZeroOverPrecisionAndOverflowAmounts() {
        assertThatThrownBy(() -> Money.requirePositive(null)).isInstanceOf(IllegalArgumentException.class);
        for (String value : new String[]{"-1", "0", "0.001", "10000000000000.00"}) {
            assertThatThrownBy(() -> Money.requirePositive(new BigDecimal(value)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void preservesCentsAndSupportsZeroBalancesWithoutRounding() {
        assertThat(Money.requirePositive(new BigDecimal("1.2"))).isEqualTo(new BigDecimal("1.20"));
        assertThat(Money.requirePositive(new BigDecimal("1.200"))).isEqualTo(new BigDecimal("1.20"));
        assertThat(Money.requireNonNegative(BigDecimal.ZERO)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.requirePositive(Money.MAX_AMOUNT)).isEqualTo(Money.MAX_AMOUNT);
    }
}
