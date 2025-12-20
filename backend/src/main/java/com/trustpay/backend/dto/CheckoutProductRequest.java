package com.trustpay.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutProductRequest(
        @NotNull UUID productId,
        @Min(1) int quantity
) {}