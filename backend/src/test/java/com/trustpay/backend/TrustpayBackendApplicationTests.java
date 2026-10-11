package com.trustpay.backend;

import com.jayway.jsonpath.JsonPath;
import com.trustpay.backend.entity.Product;
import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.enums.Role;
import com.trustpay.backend.exception.InsufficientBalanceException;
import com.trustpay.backend.repository.ProductRepository;
import com.trustpay.backend.repository.TransactionRepository;
import com.trustpay.backend.repository.UserRepository;
import com.trustpay.backend.security.JwtService;
import com.trustpay.backend.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TrustpayBackendApplicationTests {
    @Value("${local.server.port}") private int port;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransactionService transactionService;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void registrationLoginAndCurrentAccountWorkWithNormalizedEmail() throws Exception {
        String email = "Mixed." + UUID.randomUUID() + "@example.com";
        HttpResponse<String> registered = request("POST", "/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"Trustpay123!\"}");
        assertThat(registered.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(registered.body(), "$.user.email")).isEqualTo(email.toLowerCase());
        String token = JsonPath.read(registered.body(), "$.token");
        HttpResponse<String> profile = request("GET", "/users/me", token, null);
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.body()).doesNotContain("password");
        assertThat(request("POST", "/auth/login", null,
                "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"Trustpay123!\"}").statusCode())
                .isEqualTo(200);
        assertThat(request("POST", "/auth/register", null,
                "{\"email\":\"" + email.toLowerCase() + "\",\"password\":\"Trustpay123!\"}").statusCode())
                .isEqualTo(409);
    }

    @Test
    void malformedAndInvalidFinancialRequestsReturn400() throws Exception {
        User admin = account(Role.ADMIN, "100.00");
        String token = token(admin);
        assertThat(request("POST", "/transactions/transfer", token, "{}").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/transactions/transfer", token,
                "{\"toUserId\":\"invalid-uuid\",\"amount\":1}").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/admin/transactions/deposit", token,
                "{\"userId\":\"" + admin.getId() + "\",\"amount\":1.001}").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/admin/transactions/deposit", token, "{").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/loans/create?companyId=broken&amount=1&installments=1", token, null)
                .statusCode()).isEqualTo(400);
        assertThat(request("POST", "/loans/create?companyId=" + UUID.randomUUID() + "&amount=1&installments=0", token, null)
                .statusCode()).isEqualTo(400);
        assertThat(request("POST", "/store/checkout", token,
                "{\"products\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":0}],"
                        + "\"paymentMethod\":\"ACCOUNT_BALANCE\"}").statusCode()).isEqualTo(400);
    }

    @Test
    void missingInvalidAndBlockedAuthenticationAreRejected() throws Exception {
        assertThat(request("GET", "/users/me", null, null).statusCode()).isEqualTo(401);
        assertThat(request("GET", "/users/me", "invalid-token", null).statusCode()).isEqualTo(401);
        User blocked = account(Role.USER, "0.00");
        String previousToken = token(blocked);
        blocked.setStatus(AccountStatus.BLOCKED);
        users.save(blocked);
        assertThat(request("GET", "/users/me", previousToken, null).statusCode()).isEqualTo(401);
        User regular = account(Role.USER, "0.00");
        assertThat(request("POST", "/admin/transactions/deposit", token(regular),
                "{\"userId\":\"" + regular.getId() + "\",\"amount\":1}").statusCode()).isEqualTo(403);
    }

    @Test
    void checkoutChargesAndCreditsStoreAndPreservesPurchasedItems() throws Exception {
        User buyer = account(Role.USER, "100.00");
        User storeBefore = users.findByEmail("store@trustpay.com").orElseThrow();
        Product product = products.save(Product.builder().name("Snapshot product").price(new BigDecimal("10.00"))
                .stock(5).build());
        String body = "{\"products\":[{\"productId\":\"" + product.getId() + "\",\"quantity\":2}],"
                + "\"paymentMethod\":\"ACCOUNT_BALANCE\"}";
        assertThat(request("POST", "/store/checkout", token(buyer), body).statusCode()).isEqualTo(200);
        assertThat(users.findById(buyer.getId()).orElseThrow().getBalance()).isEqualByComparingTo("80.00");
        assertThat(users.findById(storeBefore.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(storeBefore.getBalance().add(new BigDecimal("20.00")));
        Product changed = products.findById(product.getId()).orElseThrow();
        assertThat(changed.getStock()).isEqualTo(3);
        changed.setName("New catalog name");
        changed.setPrice(new BigDecimal("999.00"));
        products.save(changed);
        HttpResponse<String> history = request("GET", "/store/my-orders", token(buyer), null);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(history.body()).contains("Snapshot product", "PAID").doesNotContain("New catalog name");
        assertThat(((Number) JsonPath.read(history.body(), "$[0].items[0].quantity")).intValue()).isEqualTo(2);
        assertThat(request("GET", "/transactions/my", token(buyer), null).body()).contains("STORE_PURCHASE");
    }

    @Test
    void failedPaymentRollsBackStockOrderAndLedger() throws Exception {
        User buyer = account(Role.USER, "0.00");
        Product product = products.save(Product.builder().name("Rollback product").price(new BigDecimal("10.00"))
                .stock(5).build());
        long ledgerCount = transactions.count();
        assertThat(request("POST", "/store/checkout", token(buyer),
                "{\"products\":[{\"productId\":\"" + product.getId() + "\",\"quantity\":2}],"
                        + "\"paymentMethod\":\"ACCOUNT_BALANCE\"}").statusCode()).isEqualTo(409);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(transactions.count()).isEqualTo(ledgerCount);
        assertThat(request("GET", "/store/my-orders", token(buyer), null).body()).isEqualTo("[]");
    }

    @Test
    void simultaneousTransfersCannotSpendTheSameBalanceTwice() throws Exception {
        User sender = account(Role.USER, "100.00");
        User first = account(Role.USER, "0.00");
        User second = account(Role.USER, "0.00");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(first, second).stream().map(recipient -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out starting transfers");
                try {
                    transactionService.transfer(sender.getId(), recipient.getId(), new BigDecimal("80.00"));
                    return true;
                } catch (InsufficientBalanceException ex) {
                    return false;
                }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successful = 0;
            for (var task : tasks) if (task.get(15, TimeUnit.SECONDS)) successful++;
            assertThat(successful).isEqualTo(1);
        }
        assertThat(users.findById(sender.getId()).orElseThrow().getBalance()).isEqualByComparingTo("20.00");
        BigDecimal credited = users.findById(first.getId()).orElseThrow().getBalance()
                .add(users.findById(second.getId()).orElseThrow().getBalance());
        assertThat(credited).isEqualByComparingTo("80.00");
    }

    @Test
    void selfTransferCannotAlterBalanceOrCreateLedgerEntry() {
        User user = account(Role.USER, "100.00");
        long ledgerCount = transactions.count();
        assertThatThrownBy(() -> transactionService.transfer(user.getId(), user.getId(), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(users.findById(user.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(transactions.count()).isEqualTo(ledgerCount);
    }

    @Test
    void loanLifecycleEnforcesOwnershipAndSettlesEveryCent() throws Exception {
        User admin = account(Role.ADMIN, "0.00");
        User owner = account(Role.USER, "1.00");
        User foreign = account(Role.USER, "100.00");
        String suffix = UUID.randomUUID().toString();
        HttpResponse<String> partner = request("POST", "/loans/admin/companies?name=Partner" + suffix
                + "&email=" + suffix + "@example.com", token(admin), null);
        assertThat(partner.statusCode()).isEqualTo(200);
        String companyId = JsonPath.read(partner.body(), "$.id");
        HttpResponse<String> loan = request("POST", "/loans/create?companyId=" + companyId
                + "&amount=1.00&installments=3", token(owner), null);
        assertThat(loan.statusCode()).isEqualTo(200);
        String loanId = JsonPath.read(loan.body(), "$.id");
        HttpResponse<String> schedule = request("GET", "/loans/" + loanId + "/installments", token(owner), null);
        assertThat(schedule.statusCode()).isEqualTo(200);
        List<String> ids = JsonPath.read(schedule.body(), "$[*].id");
        List<Number> amounts = JsonPath.read(schedule.body(), "$[*].amount");
        BigDecimal sum = amounts.stream().map(value -> new BigDecimal(value.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1.10");
        assertThat(request("GET", "/loans/" + loanId + "/installments", token(foreign), null).statusCode())
                .isEqualTo(404);
        assertThat(request("POST", "/loans/pay-installment/" + ids.getFirst(), token(foreign), null).statusCode())
                .isEqualTo(403);
        assertThat(request("POST", "/loans/create?companyId=" + companyId
                + "&amount=1.00&installments=3", token(owner), null).statusCode()).isEqualTo(409);
        for (String id : ids) {
            assertThat(request("POST", "/loans/pay-installment/" + id, token(owner), null).statusCode()).isEqualTo(200);
        }
        assertThat(request("POST", "/loans/pay-installment/" + ids.getFirst(), token(owner), null).statusCode())
                .isEqualTo(409);
        assertThat(users.findById(owner.getId()).orElseThrow().getOutstandingBalance()).isEqualByComparingTo("0.00");
        assertThat(users.findById(owner.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0.90");
        assertThat(request("GET", "/loans/my", token(owner), null).body()).contains("PAID");
    }

    private User account(Role role, String balance) {
        return users.save(User.builder().email(UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("Trustpay123!")).role(role).balance(new BigDecimal(balance)).build());
    }

    private String token(User account) {
        return jwtService.generateToken(account.getEmail(), Map.of());
    }

    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
