package com.trustpay.backend.services;

import com.trustpay.backend.dto.CheckoutProductRequest;
import com.trustpay.backend.entity.*;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.exception.InsufficientStockException;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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
        log.info("Processing checkout for user: {}", user.getEmail());
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Product> products = new ArrayList<>();

        // Validate products and calculate total
        for (CheckoutProductRequest request : productRequests) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + request.productId()
                    ));

            if (product.getStock() < request.quantity()) {
                throw new InsufficientStockException(
                        "Not enough stock for " + product.getName() + 
                        ". Available: " + product.getStock() + 
                        ", Requested: " + request.quantity()
                );
            }

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(request.quantity()))
            );
            products.add(product);
        }

        log.info("Total amount calculated: {}", totalAmount);

        // Update stock
        for (CheckoutProductRequest request : productRequests) {
            Product product = productRepository.findById(request.productId()).orElseThrow();
            product.setStock(product.getStock() - request.quantity());
            productRepository.save(product);
            log.debug("Updated stock for product {}: new stock = {}", 
                     product.getName(), product.getStock());
        }

        // Create order
        Order order = Order.builder()
                .user(user)
                .products(products)
                .totalAmount(totalAmount)
                .status(OrderStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("Order created successfully for user: {}", user.getEmail());
    }
}
