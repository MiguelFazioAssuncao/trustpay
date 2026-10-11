package com.trustpay.backend.services;

import com.trustpay.backend.dto.CreateCardRequest;
import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.exception.InvalidCardException;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.repository.CardRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.validation.AccountOperations;
import com.trustpay.backend.validation.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public Card create(User user, CreateCardRequest request) {
        User owner = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AccountOperations.requireActive(owner);
        if (request.type() != PaymentMethodType.CREDIT_CARD && request.type() != PaymentMethodType.DEBIT_CARD) {
            throw new InvalidCardException("Card type must be CREDIT_CARD or DEBIT_CARD");
        }
        if (request.limit() == null || request.limit().signum() < 0 || request.limit().scale() > 2
                || request.limit().compareTo(Money.MAX_AMOUNT) > 0) {
            throw new InvalidCardException("Card limit must be nonnegative and have at most two decimal places");
        }
        if (request.type() == PaymentMethodType.CREDIT_CARD && request.limit().signum() == 0) {
            throw new InvalidCardException("Credit limit must be positive");
        }
        String number = request.cardNumber();
        if (number == null || !number.matches("[0-9]{13,19}") || !passesLuhn(number)) {
            throw new InvalidCardException("Invalid card number");
        }
        // No acquiring integration needs a PAN; retain only the four display digits.
        Card card = Card.builder().cardNumber(number.substring(number.length() - 4))
                .type(request.type()).limit(request.limit()).usedLimit(BigDecimal.ZERO).user(owner).build();
        return cardRepository.save(card);
    }

    private boolean passesLuhn(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = number.length() - 1; index >= 0; index--) {
            int digit = number.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum > 0 && sum % 10 == 0;
    }
}
