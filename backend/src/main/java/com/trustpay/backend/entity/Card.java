package com.trustpay.backend.entity;

import com.trustpay.backend.enums.PaymentMethodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    private String cardNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;

    @NotNull
    @Column(name = "card_limit")
    private BigDecimal limit;

    @ManyToOne
    private User user;
}
