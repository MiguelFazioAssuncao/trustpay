package com.trustpay.backend.pattern;

import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.User;

public interface PaymentStrategy {
    void pay(User user, Order order);
}
