package com.trustpay.backend.services;

import com.trustpay.backend.entity.*;
import com.trustpay.backend.enums.InstallmentStatus;
import com.trustpay.backend.enums.LoanStatus;
import com.trustpay.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public Loan createLoan(
            User user,
            UUID companyId,
            BigDecimal amount,
            int installments
    ) {
        if (loanRepository.existsByUserAndStatus(user, LoanStatus.ACTIVE)) {
            throw new RuntimeException("User already has an active loan");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        BigDecimal interestRate = BigDecimal.valueOf(0.10);
        BigDecimal totalAmount = amount.multiply(
                BigDecimal.ONE.add(interestRate)
        );

        user.setBalance(user.getBalance().add(amount));
        user.setOutstandingBalance(user.getOutstandingBalance().add(totalAmount));
        userRepository.save(user);

        Loan loan = Loan.builder()
                .user(user)
                .company(company)
                .principalAmount(amount)
                .totalAmount(totalAmount)
                .totalInstallments(installments)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        loanRepository.save(loan);

        BigDecimal installmentAmount = totalAmount.divide(
                BigDecimal.valueOf(installments),
                2,
                RoundingMode.HALF_UP
        );

        for (int i = 1; i <= installments; i++) {
            installmentRepository.save(
                    LoanInstallment.builder()
                            .loan(loan)
                            .installmentNumber(i)
                            .amount(installmentAmount)
                            .dueDate(LocalDate.now().plusMonths(i))
                            .status(InstallmentStatus.PENDING)
                            .build()
            );
        }
        return loan;
    }

    @Transactional
    public void payInstallment(User user, UUID installmentId) {
        LoanInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        if (installment.getStatus() != InstallmentStatus.PENDING) {
            throw new RuntimeException("Installment already paid");
        }

        if (user.getBalance().compareTo(installment.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        user.setBalance(user.getBalance().subtract(installment.getAmount()));
        user.setOutstandingBalance(
                user.getOutstandingBalance().subtract(installment.getAmount())
        );

        installment.setStatus(InstallmentStatus.PAID);
        installment.setPaidAt(LocalDateTime.now());

        userRepository.save(user);
        installmentRepository.save(installment);

        boolean allPaid = installmentRepository
                .findByLoanIdAndStatus(
                        installment.getLoan().getId(),
                        InstallmentStatus.PENDING
                ).isEmpty();

        if (allPaid) {
            Loan loan = installment.getLoan();
            loan.setStatus(LoanStatus.PAID);
            loanRepository.save(loan);
        }
    }
}
