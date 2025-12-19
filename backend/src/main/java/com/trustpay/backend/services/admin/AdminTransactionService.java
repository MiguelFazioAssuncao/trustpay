package com.trustpay.backend.services.admin;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void deposit(UUID userId, BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBalance(user.getBalance().add(amount));

        Transaction tx = Transaction.builder()
                .fromUser(null)
                .toUser(user)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        userRepository.save(user);
    }
}
