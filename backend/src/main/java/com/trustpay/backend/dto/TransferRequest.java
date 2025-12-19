package com.trustpay.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        UUID toUserId,
        BigDecimal amount
) {}
