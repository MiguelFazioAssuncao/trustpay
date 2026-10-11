package com.trustpay.backend.services.impl;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.exception.EmailAlreadyExistsException;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    private UserRepository repository;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        service = new AuthServiceImpl(repository, encoder, jwt);
    }

    @Test
    void normalizesEmailForLogin() {
        User user = user(AccountStatus.ACTIVE, Role.USER);
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Password1!", "hash")).thenReturn(true);
        when(jwt.generateToken(eq(user.getEmail()), anyMap())).thenReturn("token");

        assertEquals("token", service.login("  USER@example.com  ", "Password1!"));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"BLOCKED", "CLOSED"})
    void preventsLoginForInactiveAccounts(AccountStatus status) {
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user(status, Role.USER)));
        when(encoder.matches("Password1!", "hash")).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> service.login("user@example.com", "Password1!"));
        verifyNoInteractions(jwt);
    }

    @Test
    void internalStoreCannotLoginOrRegister() {
        when(repository.findByEmailIgnoreCase("store@trustpay.com"))
                .thenReturn(Optional.of(user(AccountStatus.ACTIVE, Role.STORE)));
        when(encoder.matches("Password1!", "hash")).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> service.login("store@trustpay.com", "Password1!"));
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.register(" STORE@trustpay.com ", "Password1!"));
        verify(repository, never()).save(any());
        verifyNoInteractions(jwt);
    }

    @Test
    void normalizesRegistrationAndNeverStoresPlaintextPassword() {
        when(encoder.encode("Password1!")).thenReturn("hash");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            assertEquals("user@example.com", user.getEmail());
            assertEquals("hash", user.getPassword());
            assertEquals(Role.USER, user.getRole());
            return user;
        });
        when(jwt.generateToken(eq("user@example.com"), anyMap())).thenReturn("token");

        assertEquals("token", service.register("  USER@example.com ", "Password1!"));
        verify(repository).existsByEmailIgnoreCase("user@example.com");
    }

    @Test
    void refusesRegistrationOfLegacyMixedCaseEmail() {
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.register("USER@example.com", "Password1!"));
        verify(repository, never()).save(any());
    }

    private User user(AccountStatus status, Role role) {
        return User.builder().id(UUID.randomUUID()).email("user@example.com").password("hash")
                .role(role).status(status).build();
    }
}
