package com.trustpay.backend.controller;

import com.trustpay.backend.dto.CreateCardRequest;
import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.repository.CardRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.services.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CardController {

    private final CardService cardService;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Card> create(@RequestBody @Valid CreateCardRequest request, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(cardService.create(user, request));
    }

    @GetMapping
    public List<Card> myCards(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return cardRepository.findByUser(user);
    }
}