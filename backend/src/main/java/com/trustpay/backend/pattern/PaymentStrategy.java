package com.trustpay.backend.pattern;

import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.User;

public interface PaymentStrategy {
    void pay(User buyer, User store, Order order, Card card);
}
