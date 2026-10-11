package com.trustpay.backend.services;

import com.trustpay.backend.dto.CheckoutProductRequest;
import com.trustpay.backend.dto.response.OrderResponse;
import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.Order;
import com.trustpay.backend.entity.OrderItem;
import com.trustpay.backend.entity.Product;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.exception.InsufficientStockException;
import com.trustpay.backend.exception.InvalidCardException;
import com.trustpay.backend.exception.ResourceNotFoundException;
import com.trustpay.backend.pattern.AccountBalancePaymentStrategy;
import com.trustpay.backend.pattern.CardPaymentStrategy;
import com.trustpay.backend.repository.CardRepository;
import com.trustpay.backend.repository.OrderRepository;
import com.trustpay.backend.repository.ProductRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.validation.AccountOperations;
import com.trustpay.backend.validation.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final AccountBalancePaymentStrategy balancePaymentStrategy;
    private final CardPaymentStrategy cardPaymentStrategy;

    @Transactional
    public void checkout(User user, List<CheckoutProductRequest> productRequests,
                         PaymentMethodType paymentMethod, UUID cardId, Integer installments) {
        validatePaymentOptions(paymentMethod, cardId, installments);
        Map<UUID, Integer> quantities = aggregateQuantities(productRequests);
        UUID storeId = userRepository.findIdByEmail("store@trustpay.com")
                .orElseThrow(() -> new ResourceNotFoundException("Store account not found"));
        if (user.getId().equals(storeId)) {
            throw new IllegalArgumentException("Store account cannot buy from itself");
        }
        // Same global lock ordering as transfers, irrespective of the incoming product order.
        Map<UUID, User> accounts = new HashMap<>();
        List.of(user.getId(), storeId).stream().sorted().forEach(id ->
                accounts.put(id, userRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"))));
        User buyer = accounts.get(user.getId());
        User store = accounts.get(storeId);
        AccountOperations.requireActive(buyer);
        AccountOperations.requireActive(store);
        if (store.getRole() != Role.STORE) {
            throw new IllegalArgumentException("Invalid store account configuration");
        }

        Card card = null;
        if (paymentMethod != PaymentMethodType.ACCOUNT_BALANCE) {
            card = cardRepository.findByIdForUpdate(cardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
            if (!card.getUser().getId().equals(buyer.getId()) || card.getType() != paymentMethod) {
                throw new InvalidCardException("Card must belong to the user and match the payment method");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        List<Product> products = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        Order order = Order.builder().user(buyer).products(products).items(items)
                .paymentMethod(paymentMethod).cardId(cardId).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + entry.getKey()));
            if (product.getStock() < entry.getValue()) {
                throw new InsufficientStockException("Not enough stock for " + product.getName());
            }
            Money.requirePositive(product.getPrice());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            Money.requirePositive(total);
            products.add(product);
            items.add(OrderItem.builder().order(order).productId(product.getId()).productName(product.getName())
                    .quantity(entry.getValue()).unitPrice(product.getPrice()).build());
        }
        order.setTotalAmount(total);
        orderRepository.save(order);
        if (paymentMethod == PaymentMethodType.ACCOUNT_BALANCE) {
            balancePaymentStrategy.pay(buyer, store, order, null);
        } else {
            cardPaymentStrategy.pay(buyer, store, order, card);
        }
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            product.setStock(product.getStock() - items.get(index).getQuantity());
            productRepository.save(product);
        }
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID userId) {
        return orderRepository.findAllByUserId(userId).stream().map(OrderResponse::fromEntity).toList();
    }

    private Map<UUID, Integer> aggregateQuantities(List<CheckoutProductRequest> requests) {
        if (requests == null || requests.isEmpty() || requests.size() > 100) {
            throw new IllegalArgumentException("Checkout requires between 1 and 100 product entries");
        }
        Map<UUID, Integer> quantities = new TreeMap<>();
        for (CheckoutProductRequest request : requests) {
            if (request == null || request.productId() == null || request.quantity() <= 0) {
                throw new IllegalArgumentException("Product IDs and positive quantities are required");
            }
            try {
                quantities.merge(request.productId(), request.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Product quantity is too large");
            }
        }
        return quantities;
    }

    private void validatePaymentOptions(PaymentMethodType method, UUID cardId, Integer installments) {
        if (method == null) throw new IllegalArgumentException("Payment method is required");
        if (installments != null && installments != 1) {
            throw new IllegalArgumentException("Only single-payment checkout is supported");
        }
        if (method == PaymentMethodType.ACCOUNT_BALANCE && cardId != null) {
            throw new IllegalArgumentException("Account balance payment does not accept a card");
        }
        if (method != PaymentMethodType.ACCOUNT_BALANCE && cardId == null) {
            throw new InvalidCardException("Card ID is required for card payments");
        }
    }
}
