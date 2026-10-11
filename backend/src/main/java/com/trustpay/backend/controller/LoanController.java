package com.trustpay.backend.controller;

import com.trustpay.backend.dto.response.CompanyResponse;
import com.trustpay.backend.dto.response.LoanInstallmentResponse;
import com.trustpay.backend.dto.response.LoanResponse;
import com.trustpay.backend.entity.Loan;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.LoanService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserRepository userRepository;

    @PostMapping("/admin/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> createCompany(
            @RequestParam @NotBlank @Size(max = 100) String name,
            @RequestParam @NotBlank @Email @Size(max = 100) String email
    ) {
        return ResponseEntity.ok(loanService.createCompany(name, email));
    }


    @PostMapping("/create")
    public ResponseEntity<LoanResponse> createLoan(
            @RequestParam UUID companyId,
            @RequestParam @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @RequestParam @Min(1) @Max(360) int installments,
            Authentication auth
    ) {
        User user = currentUser(auth);
        Loan loan = loanService.createLoan(user, companyId, amount, installments);
        return ResponseEntity.ok(LoanResponse.fromEntity(loan));
    }


    @PostMapping("/pay-installment/{id}")
    public void payInstallment(
            @PathVariable UUID id,
            Authentication auth
    ) {
        loanService.payInstallment(currentUser(auth), id);
    }

    @GetMapping("/my")
    public List<LoanResponse> myLoans(Authentication auth) {
        return loanService.getMyLoans(currentUser(auth));
    }

    @GetMapping("/{id}/installments")
    public List<LoanInstallmentResponse> installments(@PathVariable UUID id, Authentication auth) {
        return loanService.getInstallments(currentUser(auth), id);
    }

    @GetMapping("/companies")
    public List<CompanyResponse> companies() {
        return loanService.getCompanies();
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
