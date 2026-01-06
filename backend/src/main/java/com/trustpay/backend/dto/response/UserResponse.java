package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;

import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        BigDecimal balance,
        BigDecimal outstandingBalance,
        AccountStatus status,
        Role role
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getBalance(),
                user.getOutstandingBalance(),
                user.getStatus(),
                user.getRole()
        );
    }
}
