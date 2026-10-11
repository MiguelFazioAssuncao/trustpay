package com.trustpay.backend.bootstrap;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.security.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class StoreAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User store = userRepository.findByEmailIgnoreCase(EmailAddress.STORE_EMAIL).orElse(null);
        if (store != null) {
            if (store.getRole() != Role.STORE) {
                throw new IllegalStateException("The reserved store email must belong to an internal STORE account");
            }
            if (store.getPassword() == null || !store.getPassword().matches("\\$2[aby]\\$\\d{2}\\$.{53}")) {
                store.setPassword(unusablePassword());
                userRepository.save(store);
            }
            return;
        }

        store = User.builder()
                .email(EmailAddress.STORE_EMAIL)
                .password(unusablePassword())
                .balance(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .role(Role.STORE)
                .status(AccountStatus.ACTIVE)
                .build();
        userRepository.save(store);
    }

    private String unusablePassword() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return passwordEncoder.encode(Base64.getEncoder().encodeToString(secret));
    }
}
