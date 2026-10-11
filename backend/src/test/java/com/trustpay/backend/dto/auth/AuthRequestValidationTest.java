package com.trustpay.backend.dto.auth;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthRequestValidationTest {
    @Test
    void normalizesEmailBeforeValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new RegisterRequest(" USER@example.com ", "Password123!");
            assertEquals("user@example.com", request.email());
            assertTrue(factory.getValidator().validate(request).isEmpty());
            assertEquals("user@example.com", new LoginRequest(" USER@example.com ", "Password123!").email());
        }
    }

    @Test
    void rejectsPasswordsWhoseUtf8EncodingExceedsBcryptLimit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            String password = "Password1!" + "é".repeat(32);
            assertTrue(password.length() <= 60);
            assertTrue(factory.getValidator().validate(new RegisterRequest("user@example.com", password)).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
                            && violation.getMessage().contains("72 UTF-8 bytes")));
            assertFalse(factory.getValidator().validate(new LoginRequest("user@example.com", password)).isEmpty());
        }
    }
}
