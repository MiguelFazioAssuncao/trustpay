package com.trustpay.backend.controller.admin;

import com.trustpay.backend.services.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PatchMapping("/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> promoteAdmin(@PathVariable UUID userId) {
            adminUserService.promoteToAdmin(userId);
            return ResponseEntity.noContent().build();
    }
}
