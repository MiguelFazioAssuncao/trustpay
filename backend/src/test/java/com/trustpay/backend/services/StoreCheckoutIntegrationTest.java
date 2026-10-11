package com.trustpay.backend.services;

import com.trustpay.backend.dto.CheckoutProductRequest;
import com.trustpay.backend.dto.CheckoutRequest;
import com.trustpay.backend.dto.CreateCardRequest;
import com.trustpay.backend.dto.response.CardResponse;
import com.trustpay.backend.dto.response.OrderResponse;
import com.trustpay.backend.entity.Card;
import com.trustpay.backend.entity.Product;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.OrderStatus;
import com.trustpay.backend.enums.PaymentMethodType;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.exception.InsufficientBalanceException;
import com.trustpay.backend.exception.InsufficientStockException;
import com.trustpay.backend.exception.InvalidCardException;
import com.trustpay.backend.exception.UnauthorizedOperationException;
import com.trustpay.backend.repository.CardRepository;
import com.trustpay.backend.repository.OrderRepository;
import com.trustpay.backend.repository.ProductRepository;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StoreCheckoutIntegrationTest {
    @Autowired StoreService storeService;
    @Autowired CardService cardService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired CardRepository cardRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired Validator validator;

    private User buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        buyer = createBuyer(new BigDecimal("100.00"));
        product = productRepository.save(Product.builder().name("Original product")
                .price(new BigDecimal("10.00")).stock(5).build());
    }

    @Test
    void balanceCheckoutAggregatesProductsAndTransfersFundsWithAnOrderReference() {
        BigDecimal storeBalance = store().getBalance();
        storeService.checkout(buyer, List.of(item(2), item(1)), PaymentMethodType.ACCOUNT_BALANCE, null, null);

        assertThat(reloadBuyer().getBalance()).isEqualByComparingTo("70.00");
        assertThat(store().getBalance()).isEqualByComparingTo(storeBalance.add(new BigDecimal("30.00")));
        assertThat(reloadProduct().getStock()).isEqualTo(2);
        OrderResponse order = storeService.getMyOrders(buyer.getId()).getFirst();
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().getFirst().quantity()).isEqualTo(3);
        assertThat(order.items().getFirst().subtotal()).isEqualByComparingTo("30.00");
        assertThat(transactionRepository.findByFromUserOrToUser(buyer, buyer)).singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getAmount()).isEqualByComparingTo("30.00");
                    assertThat(transaction.getReferenceId()).isEqualTo(order.id());
                    assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethodType.ACCOUNT_BALANCE);
                });
    }

    @Test
    void insufficientBalanceRollsBackOrderStockAndMerchantCredit() {
        buyer.setBalance(new BigDecimal("5.00"));
        buyer = userRepository.save(buyer);
        BigDecimal merchantBalance = store().getBalance();

        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(1)),
                PaymentMethodType.ACCOUNT_BALANCE, null, null)).isInstanceOf(InsufficientBalanceException.class);

        assertThat(reloadProduct().getStock()).isEqualTo(5);
        assertThat(reloadBuyer().getBalance()).isEqualByComparingTo("5.00");
        assertThat(store().getBalance()).isEqualByComparingTo(merchantBalance);
        assertThat(storeService.getMyOrders(buyer.getId())).isEmpty();
        assertThat(transactionRepository.findByFromUserOrToUser(buyer, buyer)).isEmpty();
    }

    @Test
    void repeatedProductCannotBypassStockValidation() {
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(3), item(3)),
                PaymentMethodType.ACCOUNT_BALANCE, null, null)).isInstanceOf(InsufficientStockException.class);
        assertThat(reloadProduct().getStock()).isEqualTo(5);
        assertThat(reloadBuyer().getBalance()).isEqualByComparingTo("100.00");
        assertThat(storeService.getMyOrders(buyer.getId())).isEmpty();
    }

    @Test
    void orderPricesAndNamesRemainAsPurchasedWhenTheCatalogChanges() {
        storeService.checkout(buyer, List.of(item(2)), PaymentMethodType.ACCOUNT_BALANCE, null, 1);
        Product changed = reloadProduct();
        changed.setPrice(new BigDecimal("99.00"));
        changed.setName("Renamed product");
        productRepository.save(changed);

        OrderResponse history = storeService.getMyOrders(buyer.getId()).getFirst();
        assertThat(history.items().getFirst().productName()).isEqualTo("Original product");
        assertThat(history.items().getFirst().unitPrice()).isEqualByComparingTo("10.00");
        assertThat(history.products().getFirst().name()).isEqualTo("Original product");
        assertThat(history.products().getFirst().price()).isEqualByComparingTo("10.00");
        assertThat(history.totalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void debitCardUsesAccountBalanceAndCreditsTheMerchant() {
        Card card = createCard(buyer, PaymentMethodType.DEBIT_CARD, "0.00");
        BigDecimal before = store().getBalance();
        storeService.checkout(buyer, List.of(item(1)), PaymentMethodType.DEBIT_CARD, card.getId(), 1);
        assertThat(reloadBuyer().getBalance()).isEqualByComparingTo("90.00");
        assertThat(store().getBalance()).isEqualByComparingTo(before.add(new BigDecimal("10.00")));
        assertThat(transactionRepository.findByFromUserOrToUser(buyer, buyer)).singleElement()
                .satisfies(tx -> assertThat(tx.getPaymentMethod()).isEqualTo(PaymentMethodType.DEBIT_CARD));
    }

    @Test
    void creditCardConsumesRealAvailableLimitAndRecordsDebt() {
        Card card = createCard(buyer, PaymentMethodType.CREDIT_CARD, "25.00");
        BigDecimal before = store().getBalance();
        storeService.checkout(buyer, List.of(item(2)), PaymentMethodType.CREDIT_CARD, card.getId(), null);

        assertThat(reloadBuyer().getBalance()).isEqualByComparingTo("100.00");
        assertThat(reloadBuyer().getOutstandingBalance()).isEqualByComparingTo("20.00");
        assertThat(store().getBalance()).isEqualByComparingTo(before.add(new BigDecimal("20.00")));
        Card reloaded = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(reloaded.getUsedLimit()).isEqualByComparingTo("20.00");
        assertThat(reloaded.getAvailableLimit()).isEqualByComparingTo("5.00");
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(1)),
                PaymentMethodType.CREDIT_CARD, card.getId(), null)).isInstanceOf(InvalidCardException.class);
        assertThat(reloadProduct().getStock()).isEqualTo(3);
        assertThat(reloadBuyer().getOutstandingBalance()).isEqualByComparingTo("20.00");
        assertThat(storeService.getMyOrders(buyer.getId())).hasSize(1);
    }

    @Test
    void checkoutRejectsAnotherUsersCardAndWrongPaymentType() {
        Card foreignCard = createCard(createBuyer(new BigDecimal("100.00")), PaymentMethodType.CREDIT_CARD, "100.00");
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(1)),
                PaymentMethodType.CREDIT_CARD, foreignCard.getId(), null)).isInstanceOf(InvalidCardException.class);
        Card ownDebit = createCard(buyer, PaymentMethodType.DEBIT_CARD, "0.00");
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(1)),
                PaymentMethodType.CREDIT_CARD, ownDebit.getId(), null)).isInstanceOf(InvalidCardException.class);
        assertThat(reloadProduct().getStock()).isEqualTo(5);
        assertThat(storeService.getMyOrders(buyer.getId())).isEmpty();
    }

    @Test
    void rejectsUnsupportedInstallmentsAndInvalidQuantitiesBeforeCreatingOrders() {
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(1)),
                PaymentMethodType.ACCOUNT_BALANCE, null, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(-1)),
                PaymentMethodType.ACCOUNT_BALANCE, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(Integer.MAX_VALUE), item(1)),
                PaymentMethodType.ACCOUNT_BALANCE, null, null)).isInstanceOf(IllegalArgumentException.class);
        CheckoutRequest invalid = new CheckoutRequest(List.of(new CheckoutProductRequest(null, 0)),
                PaymentMethodType.ACCOUNT_BALANCE, null, null);
        assertThat(validator.validate(invalid)).hasSize(2);
        assertThat(reloadProduct().getStock()).isEqualTo(5);
        assertThat(storeService.getMyOrders(buyer.getId())).isEmpty();
    }

    @Test
    void blockedAccountCannotPurchaseOrCreateCards() {
        buyer.setStatus(AccountStatus.BLOCKED);
        buyer = userRepository.save(buyer);
        assertThatThrownBy(() -> storeService.checkout(buyer, List.of(item(1)),
                PaymentMethodType.ACCOUNT_BALANCE, null, null)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> createCard(buyer, PaymentMethodType.DEBIT_CARD, "0.00"))
                .isInstanceOf(UnauthorizedOperationException.class);
        assertThat(reloadProduct().getStock()).isEqualTo(5);
    }

    @Test
    void cardCreationValidatesNumbersAndStoresOnlyTheLastFourDigits() {
        Card saved = createCard(buyer, PaymentMethodType.CREDIT_CARD, "100.00");
        assertThat(cardRepository.findById(saved.getId()).orElseThrow().getCardNumber()).isEqualTo("1111");
        assertThat(CardResponse.fromEntity(saved).cardNumber()).isEqualTo("**** **** **** 1111");
        assertThatThrownBy(() -> cardService.create(buyer, new CreateCardRequest("4111111111111112",
                PaymentMethodType.CREDIT_CARD, new BigDecimal("100.00")))).isInstanceOf(InvalidCardException.class);
        assertThatThrownBy(() -> cardService.create(buyer, new CreateCardRequest("4111111111111111",
                PaymentMethodType.ACCOUNT_BALANCE, new BigDecimal("100.00")))).isInstanceOf(InvalidCardException.class);
        assertThatThrownBy(() -> cardService.create(buyer, new CreateCardRequest("4111111111111111",
                PaymentMethodType.CREDIT_CARD, new BigDecimal("-1.00")))).isInstanceOf(InvalidCardException.class);
    }

    @Test
    void concurrentPurchasesNeverOversell() throws Exception {
        product.setStock(1);
        product = productRepository.save(product);
        User secondBuyer = createBuyer(new BigDecimal("100.00"));
        BigDecimal before = store().getBalance();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> purchaseAfter(start, buyer));
            Future<Boolean> second = executor.submit(() -> purchaseAfter(start, secondBuyer));
            start.countDown();
            boolean firstPaid = first.get(15, TimeUnit.SECONDS);
            boolean secondPaid = second.get(15, TimeUnit.SECONDS);
            assertThat(firstPaid ^ secondPaid).isTrue();
        }
        assertThat(reloadProduct().getStock()).isZero();
        assertThat(store().getBalance()).isEqualByComparingTo(before.add(new BigDecimal("10.00")));
        assertThat(reloadBuyer().getBalance().add(userRepository.findById(secondBuyer.getId()).orElseThrow().getBalance()))
                .isEqualByComparingTo("190.00");
    }

    @Test
    void concurrentSuccessfulPurchasesPreserveBothMerchantCredits() throws Exception {
        User secondBuyer = createBuyer(new BigDecimal("100.00"));
        BigDecimal before = store().getBalance();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> purchaseAfter(start, buyer));
            Future<Boolean> second = executor.submit(() -> purchaseAfter(start, secondBuyer));
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(15, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(reloadProduct().getStock()).isEqualTo(3);
        assertThat(store().getBalance()).isEqualByComparingTo(before.add(new BigDecimal("20.00")));
        assertThat(reloadBuyer().getBalance()).isEqualByComparingTo("90.00");
        assertThat(userRepository.findById(secondBuyer.getId()).orElseThrow().getBalance()).isEqualByComparingTo("90.00");
    }

    private boolean purchaseAfter(CountDownLatch start, User user) throws InterruptedException {
        start.await();
        try {
            storeService.checkout(user, List.of(item(1)), PaymentMethodType.ACCOUNT_BALANCE, null, null);
            return true;
        } catch (InsufficientStockException expected) {
            return false;
        }
    }

    private CheckoutProductRequest item(int quantity) {
        return new CheckoutProductRequest(product.getId(), quantity);
    }

    private User createBuyer(BigDecimal balance) {
        return userRepository.save(User.builder().email(UUID.randomUUID() + "@test.example")
                .password("not-a-real-login-password").balance(balance).role(Role.USER).status(AccountStatus.ACTIVE).build());
    }

    private Card createCard(User owner, PaymentMethodType type, String limit) {
        return cardService.create(owner, new CreateCardRequest("4111111111111111", type, new BigDecimal(limit)));
    }

    private User reloadBuyer() {
        return userRepository.findById(buyer.getId()).orElseThrow();
    }

    private Product reloadProduct() {
        return productRepository.findById(product.getId()).orElseThrow();
    }

    private User store() {
        return userRepository.findByEmail("store@trustpay.com").orElseThrow();
    }
}
