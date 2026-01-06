package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        LocalDateTime createdAt
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromUser() != null ? transaction.getFromUser().getId() : null,
                transaction.getToUser() != null ? transaction.getToUser().getId() : null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
