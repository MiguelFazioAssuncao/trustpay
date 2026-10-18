# TrustPay — Digital banking API

Demo REST API built with Java 21 and Spring Boot 4.0.0, with PostgreSQL, JWT authentication, accounts, transfers, administrative deposits, cards, loans, and a store.

## Requirements and setup

- JDK 21.
- PostgreSQL to run the application.
- Maven Wrapper is included; installing Maven is not required. The first build needs internet access to download dependencies.

Create the database:

```sql
CREATE DATABASE trustpay;
```

In the `backend` directory, prepare the environment variables:

```bash
cd backend
cp .env.example .env
# Edit DATABASE_USERNAME and DATABASE_PASSWORD in .env.
# Load only environment files you control.
set -a
source .env
set +a
export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Spring Boot does not load `.env` automatically. Export the variables in your terminal, configure your IDE, or provide them through the process environment. Keep the generated key to reuse it across restarts; changing the key invalidates previous tokens.

The API is available at `http://localhost:8080`. The only public application endpoints are `POST /auth/register` and `POST /auth/login`. All others require:

```http
Authorization: Bearer YOUR_TOKEN
```

With the application running, open Swagger UI at [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html). The OpenAPI JSON specification is available at `http://localhost:8080/v3/api-docs`. In Swagger, click **Authorize** and paste the JWT returned by login or registration; omit the `Bearer` prefix. Swagger and the specification are public, but calls to protected endpoints still require authentication.

### First administrator

Before the first startup, set `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD`. The password must contain 12 to 60 characters, including uppercase, lowercase, a digit, and a special character accepted by validation, with no spaces and at most 72 UTF-8 bytes. After creating the administrator, disable bootstrap and remove the password from that environment.

Bootstrap does not modify existing users or automatically promote a registered account. If an administrator already exists, no new administrative account is created. `store@trustpay.com` is reserved for internal use.

### Build and tests

```bash
cd backend # Run from the repository root.
./mvnw clean verify
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Tests activate the `test` profile, use an in-memory H2 database with PostgreSQL compatibility, and have a dedicated test JWT key. The suite checks authentication, validation, concurrent transfers, checkout charging/rollback, cards, installments, and rounding. H2 does not replace validation of migrations and behavior on PostgreSQL before deployment.

The previous changes passed 72 tests with `./mvnw clean verify`. The three upgrade scripts were also run twice on PostgreSQL 17.11 against the previous version's schema with sample data. The current API started against that database with the `prod` profile and schema validation; balance/credit purchases, reading older orders, and installment payments were verified over HTTP.

### Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/trustpay` |
| `DATABASE_USERNAME` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password; configure in the environment | `postgres` in development |
| `JWT_SECRET` | Base64 key with at least 32 random bytes | Required, with no default key |
| `JWT_EXPIRATION` | Token lifetime in milliseconds; minimum 1000 | `86400000` |
| `ADMIN_BOOTSTRAP_ENABLED` | Create the first administrator | `false` |
| `ADMIN_EMAIL` | Initial administrator email | No default |
| `ADMIN_PASSWORD` | Initial administrator password | No default |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact origins | Localhost on ports 3000, 5173, and 4200; empty in the `prod` profile |
| `SERVER_PORT` | HTTP port | `8080` |
| `LOG_LEVEL` | Application log level | `INFO` |
| `SHOW_SQL` | SQL logging | `false`; enabled in the `dev` profile |

The `prod` profile requires an explicitly configured database and credentials and uses `ddl-auto=validate`. Configure the CORS origins that will access the API from a browser and expose the application over HTTPS.

### Upgrading an existing database

Schema changes are not applied automatically in the `prod` profile. With a backup in place and the application stopped, review and run the PostgreSQL scripts in this order:

```bash
psql "$POSTGRES_CONNECTION_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-core.sql
psql "$POSTGRES_CONNECTION_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-loans.sql
psql "$POSTGRES_CONNECTION_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-checkout.sql
```

`POSTGRES_CONNECTION_URL` must be a `psql` connection string, such as `postgresql://user:password@localhost:5432/trustpay`, rather than a JDBC URL. Run these commands from the repository root. The scripts target databases that already contain the previous version's tables; for a new database, the development profile creates the tables, which must be provisioned and reviewed before using `prod`.

The scripts check for inconsistencies before converting values and add indexes and new order/card fields. Historical quantities and prices cannot be reconstructed for older orders: they retain their legacy product list and have an empty `items` list. Previous purchases are not charged retroactively, and the used limit for existing cards starts at zero. Full card numbers from older records are also preserved by the script and require separate review and cleanup. The legacy `transictions` table keeps its name to preserve data.

Older loans may have installments whose sum differs from the total because of previous rounding behavior. The loan script warns about these discrepancies and includes a query to identify them; review installments and account debt before resuming payments. No financial reconciliation is applied automatically.

## Endpoints

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| GET | `/swagger-ui/index.html` | Public | Interactive interface |
| GET | `/v3/api-docs` | Public | OpenAPI JSON specification |
| POST | `/auth/register` | Public | Token and user |
| POST | `/auth/login` | Public | Token and user |
| GET | `/users/me` | USER, ADMIN | Account, balance, and current debt |
| POST | `/transactions/transfer` | USER, ADMIN | Transfer; 200 with no body |
| GET | `/transactions/my` | USER, ADMIN | History, newest first |
| POST | `/admin/transactions/deposit` | ADMIN | Deposit; 200 with no body |
| PATCH | `/admin/users/{userId}/promote` | ADMIN | Promotion; 204, idempotent |
| POST | `/cards` | USER, ADMIN | Masked card |
| GET | `/cards` | USER, ADMIN | Account holder's cards |
| GET | `/products` | Authenticated | Catalog |
| POST | `/products` | ADMIN | Created product |
| POST | `/store/checkout` | USER, ADMIN | Purchase; 200 with no body |
| GET | `/store/my-orders` | USER, ADMIN | Order history |
| POST | `/loans/admin/companies` | ADMIN | Created partner |
| GET | `/loans/companies` | USER, ADMIN | Partners sorted by name |
| POST | `/loans/create` | USER, ADMIN | Created loan |
| GET | `/loans/my` | USER, ADMIN | Account holder's loans |
| GET | `/loans/{loanId}/installments` | USER, ADMIN | Installments for the account holder's own loan |
| POST | `/loans/pay-installment/{id}` | USER, ADMIN | Payment; 200 with no body |

### Authentication

Registration and login use JSON:

```json
{"email":"user@example.com","password":"Trustpay123!"}
```

At registration, passwords must contain 8 to 60 characters, including uppercase, lowercase, a digit, and one of the special characters `@#$%^&+=!`, with no spaces. Passwords must also respect BCrypt's limit of 72 UTF-8 bytes. Login accepts legacy passwords with at least six characters. Emails are trimmed and compared without regard to letter case.

Response:

```json
{
  "token": "JWT",
  "message": "Login successful",
  "user": {
    "id": "UUID",
    "email": "user@example.com",
    "balance": 100.00,
    "outstandingBalance": 0.00,
    "status": "ACTIVE",
    "role": "USER"
  }
}
```

### Transfers, deposits, and products

`POST /transactions/transfer`:

```json
{"toUserId":"RECIPIENT_UUID","amount":100.00}
```

`POST /admin/transactions/deposit`:

```json
{"userId":"USER_UUID","amount":500.00}
```

`POST /products` accepts a dedicated DTO and does not allow client-supplied IDs or persistence fields:

```json
{"name":"Product","price":50.00,"stock":10}
```

Positive amounts range from `0.01` to `9999999999999.99`. Stock is a nonnegative integer. Accounts involved in financial operations must be active.

### Cards and checkout

`POST /cards` uses `CREDIT_CARD` or `DEBIT_CARD` (the older example values `CREDIT`/`DEBIT` were incorrect). Use only test numbers in this demo:

```json
{"cardNumber":"4111111111111111","type":"CREDIT_CARD","limit":5000.00}
```

The response contains `id`, masked `cardNumber`, `type`, `limit`, `usedLimit`, and `availableLimit`. Credit requires a positive limit; debit accepts a zero limit and uses the account balance.

`POST /store/checkout`:

```json
{
  "products": [{"productId":"PRODUCT_UUID","quantity":2}],
  "paymentMethod": "ACCOUNT_BALANCE",
  "cardId": null,
  "installments": null
}
```

The cart accepts 1 to 100 entries. Quantities for repeated products are added together. `ACCOUNT_BALANCE` does not accept `cardId`; `DEBIT_CARD` and `CREDIT_CARD` require a card owned by the user with the matching type. `installments` accepts only `null` or `1`.

Balance and debit payments deduct from the buyer and credit the store. Credit preserves the buyer's available balance, increases `outstandingBalance` and `usedLimit`, and credits the store in the local ledger. An order becomes `PAID` only after payment.

`GET /store/my-orders` retains previous fields and adds `items` and `paymentMethod`:

```json
[
  {
    "id":"ORDER_UUID",
    "userId":"USER_UUID",
    "products":[{"id":"PRODUCT_UUID","name":"Product","price":50.00,"stock":null}],
    "totalAmount":100.00,
    "status":"PAID",
    "createdAt":"2026-10-10T12:00:00",
    "items":[{"productId":"PRODUCT_UUID","productName":"Product","quantity":2,"unitPrice":50.00,"subtotal":100.00}],
    "paymentMethod":"ACCOUNT_BALANCE"
  }
]
```

For new orders, `products` also uses the purchase snapshot; `stock` is `null` because current stock is not part of the historical record.

### Loans

The existing creation endpoints still receive URL parameters:

```http
POST /loans/admin/companies?name=Partner&email=partner@example.com
POST /loans/create?companyId=PARTNER_UUID&amount=1000.00&installments=12
```

Partner names and emails are validated; duplicate registrations return 409. Loans have a fixed 10% interest rate, 1 to 360 installments, and each installment must be worth at least `0.01`. The financed amount is rounded to cents; remaining cents are distributed among the first installments.

Query `/loans/my` for the loan ID and `/loans/{loanId}/installments` for installment IDs:

```json
[
  {
    "id":"INSTALLMENT_UUID",
    "installmentNumber":1,
    "amount":91.67,
    "dueDate":"2026-11-10",
    "status":"PENDING",
    "paidAt":null
  }
]
```

Pay with `POST /loans/pay-installment/INSTALLMENT_UUID`. Payment reduces the account holder's balance and debt; the final installment closes the loan as `PAID`. Querying another holder's installments returns 404; attempting to pay them returns 403.

## Errors

```json
{
  "error":"VALIDATION_ERROR",
  "message":"Validation failed",
  "timestamp":"2026-10-10T12:00:00",
  "fields":{"amount":"must be greater than or equal to 0.01"}
}
```

`fields` appears in request body validation errors. Messages are in English.

| HTTP | Main codes |
| --- | --- |
| 400 | `VALIDATION_ERROR`, `BAD_REQUEST`, `INVALID_CARD` |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | `ACCESS_DENIED`, `UNAUTHORIZED_OPERATION` |
| 404 | `RESOURCE_NOT_FOUND` |
| 405 | `METHOD_NOT_ALLOWED` |
| 409 | `EMAIL_ALREADY_EXISTS`, `INSUFFICIENT_BALANCE`, `INSUFFICIENT_STOCK`, `CONFLICT`, `CONCURRENT_UPDATE` |
| 415 | `UNSUPPORTED_MEDIA_TYPE` |
| 500 | `INTERNAL_SERVER_ERROR` |

## cURL example

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"Trustpay123!"}'

curl http://localhost:8080/users/me -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/products -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/loans/my -H "Authorization: Bearer $TOKEN"
```

Set `TOKEN` to the token returned by registration or login.

## Structure

```text
backend/
├── src/main/java/com/trustpay/backend/
│   ├── bootstrap/      # Initialize the administrator and store account
│   ├── config/         # Security and CORS
│   ├── controller/     # HTTP endpoints
│   ├── dto/            # Validated inputs and responses
│   ├── entity/         # JPA model and historical order items
│   ├── exception/      # Domain errors and HTTP handling
│   ├── pattern/        # Payment strategies
│   ├── repository/     # Persistence, queries, and locking
│   ├── security/       # JWT and authentication
│   ├── services/       # Business rules and transactions
│   └── validation/     # Monetary values and account status
├── src/main/resources/ # Profiles and logging
├── src/test/           # Unit tests and HTTP/H2 integration tests
├── scripts/            # Manual upgrades for existing databases
└── pom.xml
```

## Demo limitations

Cards and loans are local simulations: there is no integration with payment processors, credit assessment, real card issuance, or credit card bill payment. Users supply their own credit limits, and card debt is included in `outstandingBalance`. Installment checkout is not implemented. Do not use real cards.

Write endpoints do not yet have idempotency keys: retrying a purchase, deposit, or transfer after a timeout may create another operation. History endpoints return unpaginated lists. Real financial use also requires reconciliation/an accounting ledger, auditing, operational limits, abuse protection, account recovery, and versioned migrations.

A personal demonstration project by Miguel Fazio Assuncao.
