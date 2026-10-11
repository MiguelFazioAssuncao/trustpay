package com.trustpay.backend.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Validates amounts before arithmetic so persistence never silently rounds cents. */
public final class Money {
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

    private Money() {}

    public static BigDecimal requirePositive(BigDecimal amount) {
        BigDecimal value = requireNonNegative(amount);
        if (value.signum() == 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return value;
    }

    public static BigDecimal requireNonNegative(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount must be between 0 and " + MAX_AMOUNT);
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }
    }
}
