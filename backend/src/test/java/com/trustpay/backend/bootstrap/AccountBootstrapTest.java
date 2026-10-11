package com.trustpay.backend.bootstrap;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountBootstrapTest {
    private static ValidatorFactory validatorFactory;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeAll
    static void initializeValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void refusesDefaultOrInvalidAdminCredentials() {
        var repository = mock(UserRepository.class);
        assertThrows(IllegalStateException.class, () -> admin(repository, "admin@trustpay.com", "admin123").run());
        assertThrows(IllegalStateException.class, () -> admin(repository, "invalid", "SafePassword123!").run());
        assertThrows(IllegalStateException.class, () -> admin(repository, "", "").run());
        verify(repository, never()).save(any());
    }

    @Test
    void neverPromotesAnExistingAccountDuringBootstrap() {
        var repository = mock(UserRepository.class);
        when(repository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> admin(repository, "ADMIN@example.com", "SafePassword123!").run());
        verify(repository, never()).save(any());
    }

    @Test
    void createsNormalizedAdministratorWithEncodedPassword() {
        var repository = mock(UserRepository.class);
        admin(repository, " ADMIN@example.com ", "SafePassword123!").run();
        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User created = captor.getValue();
        assertEquals("admin@example.com", created.getEmail());
        assertEquals(Role.ADMIN, created.getRole());
        assertTrue(encoder.matches("SafePassword123!", created.getPassword()));
    }

    @Test
    void leavesExistingAdminCredentialsUnchanged() {
        var repository = mock(UserRepository.class);
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);
        admin(repository, "", "").run();
        verify(repository, never()).save(any());
    }

    @Test
    void repairsLegacyPlaintextStorePassword() {
        var repository = mock(UserRepository.class);
        User existing = User.builder().email("store@trustpay.com").password("senhasupersegura123")
                .role(Role.STORE).status(AccountStatus.ACTIVE).build();
        when(repository.findByEmailIgnoreCase("store@trustpay.com")).thenReturn(Optional.of(existing));

        new StoreAccountInitializer(repository, encoder).run();

        verify(repository).save(existing);
        assertTrue(existing.getPassword().startsWith("$2"));
        assertFalse(encoder.matches("senhasupersegura123", existing.getPassword()));
        assertEquals(AccountStatus.ACTIVE, existing.getStatus());
    }

    @Test
    void createsInternalStoreWithRandomHashedPasswordAndRejectsEmailHijacking() {
        var repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("store@trustpay.com")).thenReturn(Optional.empty());
        new StoreAccountInitializer(repository, encoder).run();
        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals(Role.STORE, captor.getValue().getRole());
        assertTrue(captor.getValue().getPassword().startsWith("$2"));

        var hijackedRepository = mock(UserRepository.class);
        when(hijackedRepository.findByEmailIgnoreCase("store@trustpay.com"))
                .thenReturn(Optional.of(User.builder().role(Role.USER).build()));
        assertThrows(IllegalStateException.class, () -> new StoreAccountInitializer(hijackedRepository, encoder).run());
        verify(hijackedRepository, never()).save(any());
    }

    private AdminBootstrap admin(UserRepository repository, String email, String password) {
        return new AdminBootstrap(repository, encoder, validatorFactory.getValidator(), email, password);
    }
}
