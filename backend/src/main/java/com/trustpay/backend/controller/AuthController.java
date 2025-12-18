package com.trustpay.backend.controller;

import com.trustpay.backend.dto.auth.LoginRequest;
import com.trustpay.backend.dto.auth.RegisterRequest;
import com.trustpay.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        String token = authService.login(
                loginRequest.email(),
                loginRequest.password()
        );

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "message", "Login successful"
                )
        );
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        String token = authService.register(
                registerRequest.email(),
                registerRequest.password()
        );

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "message", "User registered successfully"
                )
        );
    }
}
