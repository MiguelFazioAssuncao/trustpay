package com.trustpay.backend.pattern;

import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.enums.TransactionType;
import com.trustpay.backend.exception.InvalidCardException;
import com.trustpay.backend.repository.CardRepository;
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
public class CardPaymentStrategy implements PaymentStrategy {
    private final AccountBalancePaymentStrategy balancePaymentStrategy;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /** Local ledger simulation; external card authorization is not implemented. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void pay(User buyer, User store, Order order, Card card) {
        AccountOperations.requireActive(buyer);
        AccountOperations.requireActive(store);
        Money.requirePositive(order.getTotalAmount());
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Order is not pending");
        }
        if (card == null || !card.getUser().getId().equals(buyer.getId())) {
            throw new InvalidCardException("Card does not belong to the authenticated user");
        }
        if (card.getType() != order.getPaymentMethod()) {
            throw new InvalidCardException("Card type does not match the payment method");
        }
        if (card.getType() == PaymentMethodType.DEBIT_CARD) {
            balancePaymentStrategy.pay(buyer, store, order, card);
            return;
        }
        if (card.getType() != PaymentMethodType.CREDIT_CARD) {
            throw new InvalidCardException("Unsupported card type");
        }
        if (card.getAvailableLimit().compareTo(order.getTotalAmount()) < 0) {
            throw new InvalidCardException("Insufficient available credit limit");
        }
        BigDecimal storeBalance = store.getBalance().add(order.getTotalAmount());
        BigDecimal debt = buyer.getOutstandingBalance().add(order.getTotalAmount());
        Money.requirePositive(storeBalance);
        Money.requirePositive(debt);

        card.setUsedLimit(card.getUsedLimit().add(order.getTotalAmount()));
        buyer.setOutstandingBalance(debt);
        store.setBalance(storeBalance);
        cardRepository.save(card);
        userRepository.save(buyer);
        userRepository.save(store);
        transactionRepository.save(Transaction.builder()
                .fromUser(buyer).toUser(store).amount(order.getTotalAmount())
                .type(TransactionType.STORE_PURCHASE).paymentMethod(PaymentMethodType.CREDIT_CARD)
                .status(TransactionStatus.COMPLETED).referenceId(order.getId())
                .description("Local credit purchase - Order " + order.getId())
                .createdAt(LocalDateTime.now()).build());
        order.setStatus(OrderStatus.PAID);
    }
}
