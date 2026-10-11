package com.trustpay.backend.pattern;

import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.exception.InsufficientBalanceException;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.validation.AccountOperations;
import com.trustpay.backend.validation.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountBalancePaymentStrategy implements PaymentStrategy {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /** Called only inside checkout with both account rows already locked. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void pay(User buyer, User store, Order order, Card card) {
        AccountOperations.requireActive(buyer);
        AccountOperations.requireActive(store);
        Money.requirePositive(order.getTotalAmount());
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Order is not pending");
        }
        if (buyer.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        BigDecimal storeBalance = store.getBalance().add(order.getTotalAmount());
        Money.requirePositive(storeBalance);

        buyer.setBalance(buyer.getBalance().subtract(order.getTotalAmount()));
        store.setBalance(storeBalance);
        userRepository.save(buyer);
        userRepository.save(store);
        transactionRepository.save(Transaction.builder()
                .fromUser(buyer).toUser(store).amount(order.getTotalAmount())
                .type(TransactionType.STORE_PURCHASE).paymentMethod(order.getPaymentMethod())
                .status(TransactionStatus.COMPLETED).referenceId(order.getId())
                .description("Purchased order " + order.getId()).createdAt(LocalDateTime.now()).build());
        order.setStatus(OrderStatus.PAID);
    }
}
