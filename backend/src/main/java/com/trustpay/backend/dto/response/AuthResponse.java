package com.trustpay.backend.dto.response;

public record AuthResponse(
        String token,
        String message,
        UserResponse user
) {
}
