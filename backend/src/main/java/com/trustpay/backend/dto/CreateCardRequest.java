package com.trustpay.backend.dto;

import com.trustpay.backend.enums.PaymentMethodType;

import java.math.BigDecimal;

public record CreateCardRequest(
        String cardNumber,
        PaymentMethodType type,
        BigDecimal limit
) {}
