package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.Card;

import java.math.BigDecimal;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String cardNumber,
        String type,
        BigDecimal limit
) {
    public static CardResponse fromEntity(Card card) {
        return new CardResponse(
                card.getId(),
                maskCardNumber(card.getCardNumber()),
                card.getType(),
                card.getLimit()
        );
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
