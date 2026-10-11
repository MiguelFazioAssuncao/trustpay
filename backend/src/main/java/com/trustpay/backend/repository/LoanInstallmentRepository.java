package com.trustpay.backend.repository;

import com.trustpay.backend.entity.LoanInstallment;
import com.trustpay.backend.enums.InstallmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {

    List<LoanInstallment> findByLoanIdAndStatus(UUID loanId, InstallmentStatus status);

    List<LoanInstallment> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    boolean existsByLoanIdAndStatus(UUID loanId, InstallmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select installment from LoanInstallment installment where installment.id = :id")
    Optional<LoanInstallment> findByIdForUpdate(@Param("id") UUID id);
}
