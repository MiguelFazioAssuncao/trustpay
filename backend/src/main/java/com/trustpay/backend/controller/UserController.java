package com.trustpay.backend.controller;

import com.trustpay.backend.dto.response.UserResponse;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return UserResponse.fromEntity(userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }
}
