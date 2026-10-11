package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.Order;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        List<ProductResponse> products,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        PaymentMethodType paymentMethod
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getItems().isEmpty() ? order.getProducts().stream()
                        .map(ProductResponse::fromEntity).toList() : order.getItems().stream()
                        .map(item -> new ProductResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(), null))
                        .toList(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::fromEntity).toList(),
                order.getPaymentMethod()
        );
    }
}
