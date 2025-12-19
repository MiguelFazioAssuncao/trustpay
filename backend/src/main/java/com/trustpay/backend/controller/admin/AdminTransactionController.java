package com.trustpay.backend.controller.admin;

import com.trustpay.backend.dto.admin.DepositRequest;
import com.trustpay.backend.services.admin.AdminTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {

    private final AdminTransactionService service;

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(@RequestBody DepositRequest request) {
        service.deposit(request.userId(), request.amount());
        return ResponseEntity.ok().build();
    }
}


