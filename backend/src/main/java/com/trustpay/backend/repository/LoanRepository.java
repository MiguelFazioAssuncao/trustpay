package com.trustpay.backend.repository;

import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByUserAndStatus(User user, LoanStatus status);

    @EntityGraph(attributePaths = {"company", "user"})
    List<Loan> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Loan> findByIdAndUserId(UUID id, UUID userId);

}
