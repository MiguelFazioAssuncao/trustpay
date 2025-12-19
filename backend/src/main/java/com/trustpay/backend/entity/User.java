    package com.trustpay.backend.entity;

    import com.trustpay.backend.enums.AccountStatus;
    import com.trustpay.backend.enums.Role;
    import jakarta.persistence.*;
    import jakarta.validation.constraints.Email;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.Size;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.math.BigDecimal;
    import java.util.UUID;

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Table(name = "users")
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters long")
        @Column(unique = true, nullable = false)
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 60, message = "Password must between 6 and 60 characters long")
        @Column(nullable = false)
        private String password;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    }
