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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    public void pay(User user, Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not pending");
        }

        boolean authorized = simulateCardAuthorization(order.getTotalAmount());

        TransactionStatus status = authorized ? TransactionStatus.COMPLETED : TransactionStatus.FAILED;

        User storeAccount = userRepository.findByEmail("store@trustpay.com")
                .orElseThrow(() -> new RuntimeException("Store account not found"));

        Transaction tx = Transaction.builder()
                .fromUser(user)
                .toUser(storeAccount)
                .amount(order.getTotalAmount())
                .type(TransactionType.STORE_PURCHASE)
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .status(status)
                .description("Simulated card purchase - Order " + order.getId())
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);

        if (authorized) {
            order.setStatus(OrderStatus.PAID);
        }
    }

    private boolean simulateCardAuthorization(BigDecimal amount) {
        BigDecimal FAKE_CARD_LIMIT = new BigDecimal("5000");
        return amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(FAKE_CARD_LIMIT) <= 0;
    }
}
