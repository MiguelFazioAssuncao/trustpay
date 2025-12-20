package com.trustpay.backend.repository;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByFromUserOrToUser(User from, User to);
    Optional<Transaction> findTop1ByStatusOrderByCreatedAtAsc(TransactionStatus status);
}
