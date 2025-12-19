package com.trustpay.backend.services.admin;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional
    public void promoteToAdmin(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("User is already an admin");
        }

        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }
}
