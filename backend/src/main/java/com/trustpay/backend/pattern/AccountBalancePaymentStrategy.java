package com.trustpay.backend.pattern;

import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountBalancePaymentStrategy implements PaymentStrategy {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void pay(User user, Order order) {
        if (user.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        user.setBalance(user.getBalance().subtract(order.getTotalAmount()));
        userRepository.save(user);

        User storeAccount = userRepository.findByEmail("store@trustpay.com")
                .orElseThrow(() -> new RuntimeException("Store account not found"));

        Transaction tx = Transaction.builder()
                .fromUser(user)
                .toUser(storeAccount)
                .amount(order.getTotalAmount())
                .type(TransactionType.STORE_PURCHASE)
                .paymentMethod(PaymentMethodType.ACCOUNT_BALANCE)
                .status(TransactionStatus.COMPLETED)
                .description("Purchased order " + order.getId())
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        order.setStatus(OrderStatus.PAID);
    }
}
