package com.trustpay.backend.services;

import com.trustpay.backend.dto.CreateCardRequest;
import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public Card create(User user, CreateCardRequest request) {
        Card card = Card.builder()
                .cardNumber(request.cardNumber())
                .type(request.type())
                .limit(request.limit())
                .user(user)
                .build();

        return cardRepository.save(card);
    }
}
