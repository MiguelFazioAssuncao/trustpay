package com.trustpay.backend.services;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.exception.InsufficientBalanceException;
import com.trustpay.backend.exception.ResourceNotFoundException;
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
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void transfer(UUID fromUserId, UUID toUserId, BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User from = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        User to = userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        Transaction tx = Transaction.builder()
                .fromUser(from)
                .toUser(to)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .paymentMethod(PaymentMethodType.ACCOUNT_BALANCE)
                .status(TransactionStatus.COMPLETED)
                .description("Transfer to user " + to.getEmail())
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        userRepository.save(from);
        userRepository.save(to);
    }
}
