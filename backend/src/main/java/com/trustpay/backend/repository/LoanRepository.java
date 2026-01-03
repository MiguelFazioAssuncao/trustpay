package com.trustpay.backend.repository;

import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByUserAndStatus(User user, LoanStatus status);

}
