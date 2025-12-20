package com.trustpay.backend.controller;

import com.trustpay.backend.dto.CheckoutRequest;
import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.repository.OrderRepository;
import com.trustpay.backend.repository.ProductRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkout(
            @RequestBody @Valid CheckoutRequest request,
            Authentication auth
    ) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
    public List<Order> getMyOrders(Authentication auth) {

        System.out.println("AUTH NAME = " + auth.getName());

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("USER ID = " + user.getId());

        return orderRepository.findAllByUserId(user.getId());
    }

}
