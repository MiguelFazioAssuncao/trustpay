package com.trustpay.backend.services;

import com.trustpay.backend.dto.CheckoutProductRequest;
import com.trustpay.backend.entity.*;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void checkout(
            User user,
            List<CheckoutProductRequest> productRequests,
            PaymentMethodType paymentMethod,
            UUID cardId,
            Integer installments
    ) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Product> products = new ArrayList<>();

        for (CheckoutProductRequest request : productRequests) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getStock() < request.quantity()) {
                throw new RuntimeException("Not enough stock for " + product.getName());
            }

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(request.quantity()))
            );
            products.add(product);
        }

        for (CheckoutProductRequest request : productRequests) {
            Product product = productRepository.findById(request.productId()).orElseThrow();
            product.setStock(product.getStock() - request.quantity());
            productRepository.save(product);
        }

        Order order = Order.builder()
                .user(user)
                .products(products)
                .totalAmount(totalAmount)
                .status(OrderStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
    }


}
