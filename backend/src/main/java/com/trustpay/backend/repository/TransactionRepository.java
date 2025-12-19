package com.trustpay.backend.repository;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByFromUserOrToUser(User from, User to);
}
