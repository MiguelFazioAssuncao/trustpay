package com.trustpay.backend.bootstrap;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class StoreAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        String storeEmail = "store@trustpay.com";
        if (userRepository.findByEmail(storeEmail).isEmpty()) {
            User store = User.builder()
                    .email(storeEmail)
                    .password("senhasupersegura123")
                    .balance(BigDecimal.ZERO)
                    .outstandingBalance(BigDecimal.ZERO)
                    .role(Role.STORE)
                    .status(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(store);

        }
    }
}
