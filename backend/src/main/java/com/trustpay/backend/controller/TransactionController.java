package com.trustpay.backend.controller;

import com.trustpay.backend.dto.TransferRequest;
import com.trustpay.backend.entity.Transaction;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class TransactionController {

    private final TransactionService service;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            @RequestBody TransferRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        service.transfer(user.getId(), request.toUserId(), request.amount());
        return ResponseEntity.ok().build();
    }


    @GetMapping("/my")
    public List<Transaction> myTransactions(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        return transactionRepository.findByFromUserOrToUser(user, user);
    }
}
