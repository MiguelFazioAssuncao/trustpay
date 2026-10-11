package com.trustpay.backend.services.admin;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.exception.UnauthorizedOperationException;
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

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.STORE) {
            throw new UnauthorizedOperationException("Cannot promote the internal store account");
        }

        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }
}
