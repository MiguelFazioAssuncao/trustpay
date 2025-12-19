package com.trustpay.backend.dto.admin;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
        UUID userId,
        BigDecimal amount
) {}
