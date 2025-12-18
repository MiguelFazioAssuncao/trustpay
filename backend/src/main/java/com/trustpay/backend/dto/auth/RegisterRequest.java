package com.trustpay.backend.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 60, message = "Password must be between 8 and 60 characters")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
                message = "Password must contain at least one digit, one lowercase, one uppercase, one special character and no whitespace"
        )
        String password
) {}
