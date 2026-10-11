package com.trustpay.backend.services.admin;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.validation.AccountOperations;
import com.trustpay.backend.validation.Money;
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

        amount = Money.requirePositive(amount);
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AccountOperations.requireActive(user);
        user.setBalance(Money.requireNonNegative(user.getBalance().add(amount)));

        Transaction tx = Transaction.builder()
                .fromUser(null)
                .toUser(user)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .paymentMethod(PaymentMethodType.ACCOUNT_BALANCE)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        userRepository.save(user);
    }
}
