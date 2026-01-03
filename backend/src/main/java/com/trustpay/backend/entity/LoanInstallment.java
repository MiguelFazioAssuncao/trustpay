package com.trustpay.backend.entity;

import com.trustpay.backend.enums.InstallmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_installments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Loan loan;

    private Integer installmentNumber;

    private BigDecimal amount;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private InstallmentStatus status;

    private LocalDateTime paidAt;
}
