package com.trustpay.backend.controller;

import com.trustpay.backend.dto.response.LoanResponse;
import com.trustpay.backend.entity.Company;
import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.repository.CompanyRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @PostMapping("/admin/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Company> createCompany(@RequestParam String name, @RequestParam String email) {
        Company company = companyRepository.save(Company.builder()
                .name(name)
                .email(email)
                .build());

        return ResponseEntity.ok(company);
    }


    @PostMapping("/create")
    public ResponseEntity<LoanResponse> createLoan(
            @RequestParam UUID companyId,
            @RequestParam BigDecimal amount,
            @RequestParam int installments,
            Authentication auth
    ) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Loan loan = loanService.createLoan(user, companyId, amount, installments);
        return ResponseEntity.ok(LoanResponse.fromEntity(loan));
    }


    @PostMapping("/pay-installment/{id}")
    public void payInstallment(
            @PathVariable UUID id,
            Authentication auth
    ) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow();

        loanService.payInstallment(user, id);
    }
}
