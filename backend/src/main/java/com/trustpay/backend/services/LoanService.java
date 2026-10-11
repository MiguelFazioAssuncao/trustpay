package com.trustpay.backend.services;

import com.trustpay.backend.dto.response.CompanyResponse;
import com.trustpay.backend.dto.response.LoanInstallmentResponse;
import com.trustpay.backend.dto.response.LoanResponse;
import com.trustpay.backend.entity.Company;
import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.entity.LoanInstallment;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.InstallmentStatus;
import com.trustpay.backend.enums.LoanStatus;
import com.trustpay.backend.exception.ConflictException;
import com.trustpay.backend.exception.InsufficientBalanceException;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.exception.UnauthorizedOperationException;
import com.trustpay.backend.repository.CompanyRepository;
import com.trustpay.backend.repository.LoanInstallmentRepository;
import com.trustpay.backend.repository.LoanRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.validation.AccountOperations;
import com.trustpay.backend.validation.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final BigDecimal INTEREST_MULTIPLIER = new BigDecimal("1.10");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    private static final int MAX_INSTALLMENTS = 360;

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
        BigDecimal principal = Money.requirePositive(amount);
        if (installments < 1 || installments > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException("Installments must be between 1 and " + MAX_INSTALLMENTS);
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company is required");
        }

        BigDecimal totalAmount = Money.requirePositive(
                principal.multiply(INTEREST_MULTIPLIER).setScale(2, RoundingMode.HALF_UP)
        );
        BigDecimal installmentAmount = totalAmount.divide(
                BigDecimal.valueOf(installments), 2, RoundingMode.DOWN
        );
        if (installmentAmount.signum() == 0) {
            throw new IllegalArgumentException("Each installment must be at least 0.01");
        }

        User account = lockActiveAccount(user);
        if (loanRepository.existsByUserAndStatus(account, LoanStatus.ACTIVE)) {
            throw new ConflictException("User already has an active loan");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        BigDecimal updatedBalance = Money.requirePositive(account.getBalance().add(principal));
        BigDecimal updatedOutstanding = Money.requirePositive(account.getOutstandingBalance().add(totalAmount));
        account.setBalance(updatedBalance);
        account.setOutstandingBalance(updatedOutstanding);
        userRepository.save(account);

        Loan loan = Loan.builder()
                .user(account)
                .company(company)
                .principalAmount(principal)
                .totalAmount(totalAmount)
                .totalInstallments(installments)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        loan = loanRepository.save(loan);

        // Distribute leftover cents so installments add up exactly to the financed total.
        int extraCents = totalAmount.subtract(installmentAmount.multiply(BigDecimal.valueOf(installments)))
                .movePointRight(2).intValueExact();
        LocalDate loanDate = loan.getCreatedAt().toLocalDate();
        List<LoanInstallment> schedule = new ArrayList<>(installments);
        for (int i = 1; i <= installments; i++) {
            schedule.add(
                    LoanInstallment.builder()
                            .loan(loan)
                            .installmentNumber(i)
                            .amount(i <= extraCents ? installmentAmount.add(ONE_CENT) : installmentAmount)
                            .dueDate(loanDate.plusMonths(i))
                            .status(InstallmentStatus.PENDING)
                            .build()
            );
        }
        installmentRepository.saveAll(schedule);
        return loan;
    }

    @Transactional
    public void payInstallment(User user, UUID installmentId) {
        User account = lockActiveAccount(user);
        LoanInstallment installment = installmentRepository.findByIdForUpdate(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));
        Loan loan = installment.getLoan();
        if (!loan.getUser().getId().equals(account.getId())) {
            throw new UnauthorizedOperationException("You can only pay your own loan installments");
        }

        if (installment.getStatus() != InstallmentStatus.PENDING) {
            throw new ConflictException("Installment already paid");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ConflictException("Loan is not active");
        }

        BigDecimal paymentAmount = Money.requirePositive(installment.getAmount());
        if (account.getBalance().compareTo(paymentAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        if (account.getOutstandingBalance().compareTo(paymentAmount) < 0) {
            throw new ConflictException("Outstanding balance is inconsistent with the installment amount");
        }

        account.setBalance(account.getBalance().subtract(paymentAmount));
        account.setOutstandingBalance(
                account.getOutstandingBalance().subtract(paymentAmount)
        );

        installment.setStatus(InstallmentStatus.PAID);
        installment.setPaidAt(LocalDateTime.now());

        userRepository.save(account);
        installmentRepository.saveAndFlush(installment);

        if (!installmentRepository.existsByLoanIdAndStatus(loan.getId(), InstallmentStatus.PENDING)) {
            loan.setStatus(LoanStatus.PAID);
            loanRepository.save(loan);
        }
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getMyLoans(User user) {
        return loanRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(LoanResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanInstallmentResponse> getInstallments(User user, UUID loanId) {
        loanRepository.findByIdAndUserId(loanId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        return installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(LoanInstallmentResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getCompanies() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(CompanyResponse::fromEntity).toList();
    }

    @Transactional
    public CompanyResponse createCompany(String name, String email) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("Company name must contain between 1 and 100 characters");
        }
        if (email == null || email.isBlank() || email.trim().length() > 100) {
            throw new IllegalArgumentException("Company email must contain between 1 and 100 characters");
        }
        String normalizedName = name.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (companyRepository.existsByName(normalizedName) || companyRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Company name or email already registered");
        }
        Company company = companyRepository.save(Company.builder()
                .name(normalizedName).email(normalizedEmail).build());
        return CompanyResponse.fromEntity(company);
    }

    private User lockActiveAccount(User user) {
        User account = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AccountOperations.requireActive(account);
        return account;
    }
}
