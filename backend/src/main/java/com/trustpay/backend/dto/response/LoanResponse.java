package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID userId,
        String companyName,
        BigDecimal principalAmount,
        BigDecimal totalAmount,
        Integer totalInstallments,
        LoanStatus status,
        LocalDateTime createdAt
) {
    public static LoanResponse fromEntity(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getUser().getId(),
                loan.getCompany().getName(),
                loan.getPrincipalAmount(),
                loan.getTotalAmount(),
                loan.getTotalInstallments(),
                loan.getStatus(),
                loan.getCreatedAt()
        );
    }
}
