package com.trustpay.backend.bootstrap;

import com.trustpay.backend.dto.auth.RegisterRequest;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.security.EmailAddress;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "trustpay.admin", name = "bootstrap-enabled", havingValue = "true")
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder, Validator validator,
                          @Value("${trustpay.admin.email:}") String adminEmail,
                          @Value("${trustpay.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.adminEmail = EmailAddress.normalize(adminEmail);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!validator.validate(new RegisterRequest(adminEmail, adminPassword)).isEmpty()
                || adminPassword.length() < 12) {
            throw new IllegalStateException("Admin bootstrap requires a valid ADMIN_EMAIL and an ADMIN_PASSWORD "
                    + "of 12 to 60 characters with uppercase, lowercase, digit and special character");
        }
        if (EmailAddress.STORE_EMAIL.equals(adminEmail) || userRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new IllegalStateException("ADMIN_EMAIL is reserved or already belongs to an existing account");
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
        log.info("Initial administrator account created");
    }
}
