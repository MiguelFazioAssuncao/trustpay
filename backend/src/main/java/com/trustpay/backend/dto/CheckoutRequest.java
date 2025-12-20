package com.trustpay.backend.dto;

import com.trustpay.backend.enums.PaymentMethodType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @NotEmpty List<CheckoutProductRequest> products,
        @NotNull PaymentMethodType paymentMethod,
        UUID cardId,
        Integer installments
) {}
