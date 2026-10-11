package com.trustpay.backend.dto;

import com.trustpay.backend.enums.PaymentMethodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @NotEmpty @Size(max = 100) List<@NotNull @Valid CheckoutProductRequest> products,
        @NotNull PaymentMethodType paymentMethod,
        UUID cardId,
        @Min(1) Integer installments
) {}
