package com.trustpay.backend.services.impl;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.exception.EmailAlreadyExistsException;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.security.JwtService;
import com.trustpay.backend.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid credentials")
                );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return jwtService.generateToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId().toString(),
                        "role", user.getRole().name(),
                        "status", user.getStatus().name()
                )
        );
    }

    @Override
    public String register(String email, String password) {

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .balance(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return jwtService.generateToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId().toString(),
                        "role", user.getRole().name(),
                        "status", user.getStatus().name()
                )
        );
    }

}
