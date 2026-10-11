package com.trustpay.backend.services.impl;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsServiceImplTest {
    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"BLOCKED", "CLOSED"})
    void disablesInactiveAccountPrincipals(AccountStatus status) {
        var repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user(status, Role.USER)));
        assertFalse(new UserDetailsServiceImpl(repository).loadUserByUsername(" USER@example.com ").isEnabled());
    }

    @Test
    void disablesInternalStoreAccountPrincipalsEvenWhenActive() {
        var repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user(AccountStatus.ACTIVE, Role.STORE)));
        assertFalse(new UserDetailsServiceImpl(repository).loadUserByUsername("user@example.com").isEnabled());
    }

    private User user(AccountStatus status, Role role) {
        return User.builder().email("user@example.com").password("hash").status(status).role(role).build();
    }
}
