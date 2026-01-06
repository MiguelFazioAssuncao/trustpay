package com.trustpay.backend.controller;

import com.trustpay.backend.dto.auth.LoginRequest;
import com.trustpay.backend.dto.auth.RegisterRequest;
import com.trustpay.backend.dto.response.AuthResponse;
import com.trustpay.backend.dto.response.UserResponse;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        String token = authService.login(
                loginRequest.email(),
                loginRequest.password()
        );

        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow();

        return ResponseEntity.ok(
                new AuthResponse(
                        token,
                        "Login successful",
                        UserResponse.fromEntity(user)
                )
        );
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        String token = authService.register(
                registerRequest.email(),
                registerRequest.password()
        );

        User user = userRepository.findByEmail(registerRequest.email()).orElseThrow();

        return ResponseEntity.ok(
                new AuthResponse(
                        token,
                        "User registered successfully",
                        UserResponse.fromEntity(user)
                )
        );
    }
}
