package com.trustpay.backend.services;

import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.dto.response.TransactionResponse;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.exception.InsufficientBalanceException;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void transfer(UUID fromUserId, UUID toUserId, BigDecimal amount) {

        amount = Money.requirePositive(amount);
        if (fromUserId == null || toUserId == null) {
            throw new IllegalArgumentException("Sender and receiver are required");
        }
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        // Every operation touching two accounts takes locks in the same order.
        UUID firstId = fromUserId.compareTo(toUserId) < 0 ? fromUserId : toUserId;
        UUID secondId = firstId.equals(fromUserId) ? toUserId : fromUserId;
        User first = userRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        User second = userRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        User from = firstId.equals(fromUserId) ? first : second;
        User to = firstId.equals(toUserId) ? first : second;
        AccountOperations.requireActive(from);
        AccountOperations.requireActive(to);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        BigDecimal recipientBalance = Money.requireNonNegative(to.getBalance().add(amount));
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(recipientBalance);

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

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return transactionRepository.findByFromUserOrToUserOrderByCreatedAtDescIdDesc(user, user)
                .stream().map(TransactionResponse::fromEntity).toList();
    }
}
