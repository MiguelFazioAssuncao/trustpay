package com.trustpay.backend.services;

import com.trustpay.backend.entity.Company;
import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.entity.LoanInstallment;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanServiceTest {

    private LoanRepository loans;
    private LoanInstallmentRepository installments;
    private UserRepository users;
    private CompanyRepository companies;
    private LoanService service;

    @BeforeEach
    void setUp() {
        loans = mock(LoanRepository.class);
        installments = mock(LoanInstallmentRepository.class);
        users = mock(UserRepository.class);
        companies = mock(CompanyRepository.class);
        service = new LoanService(loans, installments, users, companies);
    }

    @Test
    void createsExactCentScheduleAndUpdatesTheLockedAccount() {
        User account = account("500.00", "0.00");
        User staleAccount = account("1.00", "0.00");
        staleAccount.setId(account.getId());
        Company company = stubLoanCreation(account);

        Loan loan = service.createLoan(staleAccount, company.getId(), new BigDecimal("1000.00"), 12);

        ArgumentCaptor<Iterable<LoanInstallment>> scheduleCaptor = installmentCaptor();
        verify(installments).saveAll(scheduleCaptor.capture());
        List<LoanInstallment> schedule = new ArrayList<>();
        scheduleCaptor.getValue().forEach(schedule::add);
        assertEquals(12, schedule.size());
        assertEquals(new BigDecimal("1100.00"), schedule.stream()
                .map(LoanInstallment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("91.67"), schedule.getFirst().getAmount());
        assertEquals(new BigDecimal("91.66"), schedule.getLast().getAmount());
        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        assertEquals(new BigDecimal("1100.00"), account.getOutstandingBalance());
        assertSame(account, loan.getUser());
        assertEquals(new BigDecimal("1.00"), staleAccount.getBalance());
        for (int i = 0; i < schedule.size(); i++) {
            assertEquals(i + 1, schedule.get(i).getInstallmentNumber());
            assertEquals(loan.getCreatedAt().toLocalDate().plusMonths(i + 1L), schedule.get(i).getDueDate());
        }
        var order = inOrder(users, loans);
        order.verify(users).findByIdForUpdate(account.getId());
        order.verify(loans).existsByUserAndStatus(account, LoanStatus.ACTIVE);
    }

    @Test
    void roundsTheFinancedTotalBeforeCreatingTheSchedule() {
        User account = account("0.00", "0.00");
        Company company = stubLoanCreation(account);

        Loan loan = service.createLoan(account, company.getId(), new BigDecimal("0.05"), 3);

        assertEquals(new BigDecimal("0.06"), loan.getTotalAmount());
        ArgumentCaptor<Iterable<LoanInstallment>> captor = installmentCaptor();
        verify(installments).saveAll(captor.capture());
        captor.getValue().forEach(item -> assertEquals(new BigDecimal("0.02"), item.getAmount()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10.00", "1.001", "10000000000000"})
    void rejectsInvalidPrincipalBeforeAnyWrites(String value) {
        assertThrows(IllegalArgumentException.class, () -> service.createLoan(
                account("10.00", "0.00"), UUID.randomUUID(), new BigDecimal(value), 1));
        verifyNoInteractions(users, loans, installments, companies);
    }

    @Test
    void rejectsMissingPrincipal() {
        assertThrows(IllegalArgumentException.class, () -> service.createLoan(
                account("10.00", "0.00"), UUID.randomUUID(), null, 1));
        verifyNoInteractions(users, loans, installments, companies);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 361})
    void rejectsInvalidInstallmentCounts(int count) {
        assertThrows(IllegalArgumentException.class, () -> service.createLoan(
                account("10.00", "0.00"), UUID.randomUUID(), new BigDecimal("10.00"), count));
        verifyNoInteractions(users, loans, installments, companies);
    }

    @Test
    void rejectsSchedulesWithZeroValueInstallments() {
        assertThrows(IllegalArgumentException.class, () -> service.createLoan(
                account("10.00", "0.00"), UUID.randomUUID(), new BigDecimal("0.01"), 2));
        verifyNoInteractions(users, loans, installments, companies);
    }

    @Test
    void rejectsInterestThatWouldExceedMoneyPrecision() {
        assertThrows(IllegalArgumentException.class, () -> service.createLoan(
                account("0.00", "0.00"), UUID.randomUUID(), new BigDecimal("9999999999999.99"), 12));
        verifyNoInteractions(users, loans, installments, companies);
    }

    @Test
    void rejectsSecondActiveLoan() {
        User account = account("10.00", "11.00");
        when(users.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        when(loans.existsByUserAndStatus(account, LoanStatus.ACTIVE)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createLoan(
                account, UUID.randomUUID(), new BigDecimal("10.00"), 1));
        verify(users, never()).save(any());
        verify(loans, never()).save(any());
        verifyNoInteractions(companies, installments);
    }

    @Test
    void rejectsPaymentOfAnotherUsersInstallmentWithoutChangingBalances() {
        User payer = account("100.00", "100.00");
        User owner = account("100.00", "100.00");
        LoanInstallment installment = installment(owner, "20.00");
        when(users.findByIdForUpdate(payer.getId())).thenReturn(Optional.of(payer));
        when(installments.findByIdForUpdate(installment.getId())).thenReturn(Optional.of(installment));

        assertThrows(UnauthorizedOperationException.class, () -> service.payInstallment(payer, installment.getId()));
        assertEquals(new BigDecimal("100.00"), payer.getBalance());
        assertEquals(new BigDecimal("100.00"), payer.getOutstandingBalance());
        assertEquals(InstallmentStatus.PENDING, installment.getStatus());
        verify(users, never()).save(any());
        verify(installments, never()).saveAndFlush(any());
        verifyNoInteractions(loans);
    }

    @Test
    void paysFinalInstallmentAndSettlesTheLoan() {
        User account = account("30.00", "20.00");
        LoanInstallment installment = stubPayment(account, "20.00");

        service.payInstallment(account, installment.getId());

        assertEquals(new BigDecimal("10.00"), account.getBalance());
        assertEquals(new BigDecimal("0.00"), account.getOutstandingBalance());
        assertEquals(InstallmentStatus.PAID, installment.getStatus());
        assertNotNull(installment.getPaidAt());
        assertEquals(LoanStatus.PAID, installment.getLoan().getStatus());
        verify(loans).save(installment.getLoan());
        var order = inOrder(users, installments);
        order.verify(users).findByIdForUpdate(account.getId());
        order.verify(installments).findByIdForUpdate(installment.getId());
        order.verify(users).save(account);
        order.verify(installments).saveAndFlush(installment);
        order.verify(installments).existsByLoanIdAndStatus(installment.getLoan().getId(), InstallmentStatus.PENDING);
    }

    @Test
    void keepsLoanActiveWhileAnotherInstallmentIsPending() {
        User account = account("30.00", "40.00");
        LoanInstallment installment = stubPayment(account, "20.00");
        when(installments.existsByLoanIdAndStatus(installment.getLoan().getId(), InstallmentStatus.PENDING))
                .thenReturn(true);

        service.payInstallment(account, installment.getId());

        assertEquals(LoanStatus.ACTIVE, installment.getLoan().getStatus());
        verifyNoInteractions(loans);
    }

    @Test
    void rejectsRepeatPayment() {
        User account = account("100.00", "100.00");
        LoanInstallment installment = stubPayment(account, "20.00");
        installment.setStatus(InstallmentStatus.PAID);

        assertThrows(ConflictException.class, () -> service.payInstallment(account, installment.getId()));
        verify(users, never()).save(any());
        verify(installments, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPaymentWithInsufficientFunds() {
        User account = account("10.00", "20.00");
        LoanInstallment installment = stubPayment(account, "20.00");

        assertThrows(InsufficientBalanceException.class, () -> service.payInstallment(account, installment.getId()));
        assertEquals(new BigDecimal("10.00"), account.getBalance());
        assertEquals(InstallmentStatus.PENDING, installment.getStatus());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsInactiveAccountBeforeLockingAnInstallment() {
        User account = account("100.00", "100.00");
        account.setStatus(AccountStatus.BLOCKED);
        when(users.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedOperationException.class, () -> service.payInstallment(account, UUID.randomUUID()));
        verifyNoInteractions(installments, loans);
    }

    @Test
    void hidesInstallmentsForLoansOutsideTheAccount() {
        User account = account("100.00", "100.00");
        UUID loanId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () -> service.getInstallments(account, loanId));
        verify(loans).findByIdAndUserId(loanId, account.getId());
        verifyNoInteractions(installments);
    }

    @Test
    void normalizesPartnerFieldsBeforePersisting() {
        when(companies.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId(UUID.randomUUID());
            return company;
        });

        var response = service.createCompany("  Partner  ", " PARTNER@EXAMPLE.COM ");

        assertEquals("Partner", response.name());
        assertEquals("partner@example.com", response.email());
        verify(companies).existsByEmailIgnoreCase("partner@example.com");
    }

    @Test
    void rejectsPartnerWithPreviouslyRegisteredEmail() {
        when(companies.existsByEmailIgnoreCase("partner@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createCompany("Partner", "partner@example.com"));
        verify(companies, never()).save(any());
    }

    private User account(String balance, String outstanding) {
        return User.builder().id(UUID.randomUUID()).status(AccountStatus.ACTIVE)
                .balance(new BigDecimal(balance)).outstandingBalance(new BigDecimal(outstanding)).build();
    }

    private Company stubLoanCreation(User account) {
        Company company = Company.builder().id(UUID.randomUUID()).name("Partner").email("partner@example.com").build();
        when(users.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        when(companies.findById(company.getId())).thenReturn(Optional.of(company));
        when(loans.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(UUID.randomUUID());
            return loan;
        });
        return company;
    }

    private LoanInstallment stubPayment(User account, String amount) {
        LoanInstallment installment = installment(account, amount);
        when(users.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        when(installments.findByIdForUpdate(installment.getId())).thenReturn(Optional.of(installment));
        return installment;
    }

    private LoanInstallment installment(User owner, String amount) {
        Loan loan = Loan.builder().id(UUID.randomUUID()).user(owner).status(LoanStatus.ACTIVE).build();
        return LoanInstallment.builder().id(UUID.randomUUID()).loan(loan)
                .amount(new BigDecimal(amount)).status(InstallmentStatus.PENDING).build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Iterable<LoanInstallment>> installmentCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }
}
