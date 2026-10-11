package com.trustpay.backend.entity;

import com.trustpay.backend.enums.PaymentMethodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(indexes = @Index(name = "idx_card_user_id", columnList = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String cardNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    @Column(name = "card_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal limit;

    @Builder.Default
    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 15, scale = 2, columnDefinition = "numeric(15,2) default 0")
    private BigDecimal usedLimit = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long version;

    public BigDecimal getAvailableLimit() {
        return limit.subtract(usedLimit);
    }
}
