package com.trustpay.backend.dto;

import com.trustpay.backend.enums.PaymentMethodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateCardRequest(
        @NotBlank @Pattern(regexp = "[0-9]{13,19}", message = "Card number must contain 13 to 19 digits") String cardNumber,
        @NotNull PaymentMethodType type,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal limit
) {}
