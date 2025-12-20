package com.trustpay.backend.repository;

import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByUser(User user);
}

