package com.trustpay.backend.controller;

import com.trustpay.backend.dto.CheckoutRequest;
import com.trustpay.backend.dto.response.OrderResponse;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.repository.OrderRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkout(
            @RequestBody @Valid CheckoutRequest request,
            Authentication auth
    ) {
        log.info("Checkout request from user: {}", auth.getName());
        
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        storeService.checkout(
                user,
                request.products(),
                request.paymentMethod(),
                request.cardId(),
                request.installments()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(Authentication auth) {
        log.debug("Fetching orders for user: {}", auth.getName());
        
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findAllByUserId(user.getId()).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

}
