package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.LoanInstallment;
import com.trustpay.backend.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanInstallmentResponse(
        UUID id,
        Integer installmentNumber,
        BigDecimal amount,
        LocalDate dueDate,
        InstallmentStatus status,
        LocalDateTime paidAt
) {

    public static LoanInstallmentResponse fromEntity(LoanInstallment installment) {
        return new LoanInstallmentResponse(
                installment.getId(), installment.getInstallmentNumber(), installment.getAmount(),
                installment.getDueDate(), installment.getStatus(), installment.getPaidAt()
        );
    }
}
