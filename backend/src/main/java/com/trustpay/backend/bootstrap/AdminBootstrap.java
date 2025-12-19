package com.trustpay.backend.bootstrap;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${trustpay.admin.email}")
    private String adminEmail;

    @Value("${trustpay.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .balance(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        System.out.println("Initial admin user created: " + adminEmail);
    }
}
