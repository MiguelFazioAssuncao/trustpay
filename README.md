# 🏦 TrustPay - Digital Banking API

TrustPay is a complete RESTful API for a digital banking system, built with Spring Boot. The system provides account management, transactions, cards, loans, and an integrated online store.

## 📋 Table of Contents

- [Technologies Used](#technologies-used)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation and Setup](#installation-and-setup)
- [Environment Variables](#environment-variables)
- [Running the Project](#running-the-project)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Design Patterns](#design-patterns)

## 🚀 Technologies Used

- **Java 21** - Programming language
- **Spring Boot 4.0.0** - Main framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence
- **PostgreSQL** - Relational database
- **JWT (JSON Web Tokens)** - Stateless authentication
- **Lombok** - Boilerplate reduction
- **Maven** - Dependency management
- **Bean Validation** - Data validation

## ✨ Features

### Authentication and Authorization
- ✅ User registration
- ✅ JWT login
- ✅ Role-based access control (USER, ADMIN)

### User Management
- ✅ View account balance and details
- ✅ Promote users to admin (admins only)

### Transactions
- ✅ Transfers between users
- ✅ Deposits (admins only)
- ✅ Transaction history

### Cards
- ✅ Create virtual cards
- ✅ List user cards
- ✅ Mask card numbers in responses

### Loans
- ✅ Loan requests with partner companies
- ✅ Installment system
- ✅ Installment payments
- ✅ Register partner companies (admins only)

### Online Store
- ✅ Product listing
- ✅ Checkout with multiple payment methods
- ✅ Order history
- ✅ Product creation (admins only)
- ✅ Inventory control

## 📦 Prerequisites

- Java 21 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher
- Git (optional)

## ⚙️ Installation and Setup

### 1. Clone the repository

```bash
git clone https://github.com/MiguelFazioAssuncao/trustpay.git
cd trustpay/backend
```

### 2. Configure the PostgreSQL database

Create a database in PostgreSQL:

```sql
CREATE DATABASE trustpay;
```

### 3. Configure environment variables

Create a `.env` file in the project root or set the variables in your system:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/trustpay
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# JWT
JWT_SECRET=your_very_secure_jwt_secret_here
JWT_EXPIRATION=86400000

# Admin
ADMIN_EMAIL=admin@trustpay.com
ADMIN_PASSWORD=admin123

# Server
SERVER_PORT=8080
LOG_LEVEL=INFO
```

## 🔐 Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| DATABASE_URL | PostgreSQL connection URL | jdbc:postgresql://localhost:5432/trustpay |
| DATABASE_USERNAME | Database user | postgres |
| DATABASE_PASSWORD | Database password | postgres |
| JWT_SECRET | Secret key for JWT token generation | (unsafe default value) |
| JWT_EXPIRATION | Token expiration time in ms | 86400000 (24h) |
| ADMIN_EMAIL | Initial admin user email | admin@trustpay.com |
| ADMIN_PASSWORD | Initial admin user password | admin123 |
| SERVER_PORT | Server port | 8080 |
| LOG_LEVEL | Application log level | INFO |

## 🏃 Running the Project

### Development Mode

```bash
# Build and run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or use installed Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Build the project

```bash
# Generate the JAR
./mvnw clean package

# Run the generated JAR
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

The application will be available at: `http://localhost:8080`

## 📚 API Documentation

### Base URL
```
http://localhost:8080
```

### Authentication

All endpoints except `/auth/**` require JWT Bearer Token authentication.

**Authentication Header:**
```
Authorization: Bearer {your_jwt_token}
```

---

## 🔑 Authentication Endpoints

### Register User
```http
POST /auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "User registered successfully",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "balance": 0.00,
    "outstandingBalance": 0.00,
    "status": "ACTIVE",
    "role": "USER"
  }
}
```

### Login
```http
POST /auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "balance": 100.00,
    "outstandingBalance": 0.00,
    "status": "ACTIVE",
    "role": "USER"
  }
}
```

---

## 💳 Card Endpoints

**Requires:** Role `USER`

### Create Card
```http
POST /cards
```

**Request Body:**
```json
{
  "cardNumber": "1234567890123456",
  "type": "CREDIT",
  "limit": 5000.00
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "cardNumber": "**** **** **** 3456",
  "type": "CREDIT",
  "limit": 5000.00
}
```

### List My Cards
```http
GET /cards
```

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "cardNumber": "**** **** **** 3456",
    "type": "CREDIT",
    "limit": 5000.00
  }
]
```

---

## 💰 Transaction Endpoints

**Requires:** Role `USER` or `ADMIN`

### Transfer Money
```http
POST /transactions/transfer
```

**Request Body:**
```json
{
  "toUserId": "recipient_uuid",
  "amount": 100.00
}
```

**Response:** `200 OK`

### List My Transactions
```http
GET /transactions/my
```

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "fromUserId": "uuid",
    "toUserId": "uuid",
    "amount": 100.00,
    "type": "TRANSFER",
    "status": "COMPLETED",
    "createdAt": "2026-01-06T10:30:00"
  }
]
```

---

## 🏪 Product Endpoints

### List Products
```http
GET /products
```

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "name": "Sample Product",
    "price": 50.00,
    "stock": 100
  }
]
```

### Create Product
**Requires:** Role `ADMIN`

```http
POST /products
```

**Request Body:**
```json
{
  "name": "New Product",
  "price": 75.00,
  "stock": 50
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "name": "New Product",
  "price": 75.00,
  "stock": 50
}
```

---

## 🛒 Store Endpoints

**Requires:** Role `USER` or `ADMIN`

### Checkout
```http
POST /store/checkout
```

**Request Body:**
```json
{
  "products": [
    {
      "productId": "product_uuid",
      "quantity": 2
    }
  ],
  "paymentMethod": "ACCOUNT_BALANCE",
  "cardId": null,
  "installments": null
}
```

**Response:** `200 OK`

### List My Orders
```http
GET /store/my-orders
```

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "products": [
      {
        "id": "uuid",
        "name": "Product",
        "price": 50.00,
        "stock": 98
      }
    ],
    "totalAmount": 100.00,
    "status": "PAID",
    "createdAt": "2026-01-06T10:30:00"
  }
]
```

---

## 💸 Loan Endpoints

### Create Partner Company
**Requires:** Role `ADMIN`

```http
POST /loans/admin/companies?name=Company&email=company@example.com
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "name": "Company",
  "email": "company@example.com"
}
```

### Request Loan
```http
POST /loans/create?companyId=uuid&amount=1000&installments=12
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "userId": "uuid",
  "companyName": "Company",
  "principalAmount": 1000.00,
  "totalAmount": 1100.00,
  "totalInstallments": 12,
  "status": "ACTIVE",
  "createdAt": "2026-01-06T10:30:00"
}
```

### Pay Installment
```http
POST /loans/pay-installment/{installmentId}
```

**Response:** `200 OK`

---

## 👑 Admin Endpoints

### Promote User to Admin
**Requires:** Role `ADMIN`

```http
PATCH /admin/users/{userId}/promote
```

**Response:** `204 No Content`

### Deposit Money to Account
**Requires:** Role `ADMIN`

```http
POST /admin/transactions/deposit
```

**Request Body:**
```json
{
  "userId": "user_uuid",
  "amount": 500.00
}
```

**Response:** `200 OK`

---

## 📁 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/trustpay/backend/
│   │   │   ├── bootstrap/          # Data initialization
│   │   │   ├── config/             # Configurations (Security, CORS)
│   │   │   ├── controller/         # REST endpoints
│   │   │   │   └── admin/          # Administrative endpoints
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   ├── auth/           # Auth DTOs
│   │   │   │   ├── admin/          # Admin DTOs
│   │   │   │   └── response/       # Response DTOs
│   │   │   ├── entity/             # JPA entities
│   │   │   ├── enums/              # Enumerations
│   │   │   ├── exception/          # Custom exceptions
│   │   │   │   └── handler/        # Global Exception Handler
│   │   │   ├── pattern/            # Design patterns (Strategy)
│   │   │   ├── repository/         # JPA repositories
│   │   │   ├── scheduler/          # Scheduled tasks
│   │   │   ├── security/           # JWT and security settings
│   │   │   └── services/           # Business logic
│   │   │       ├── admin/          # Administrative services
│   │   │       └── impl/           # Service implementations
│   │   └── resources/
│   │       ├── application.properties       # Main configuration
│   │       ├── application-dev.properties   # Development profile
│   │       └── application-prod.properties  # Production profile
│   └── test/
└── pom.xml
```

## 🎯 Design Patterns

### Strategy Pattern
Implemented for different payment methods:
- `AccountBalancePaymentStrategy` - Account balance payment
- `CardPaymentStrategy` - Card payment

### DTO Pattern
Clear separation between domain entities and transfer objects:
- Request DTOs for input data
- Response DTOs for output data, protecting sensitive information

### Repository Pattern
Abstraction of the data access layer with Spring Data JPA

### Service Layer Pattern
Service layer separated for business logic

## 🔒 Security

- **JWT Authentication**: Stateless tokens for authentication
- **BCrypt**: Password hashing
- **Role-Based Access Control**: Role-based access control
- **CORS**: Configured to allow only specific origins
- **Input Validation**: Input validation with Bean Validation
- **Exception Handling**: Global exception handling

## 🐛 Error Handling

The API returns standardized error responses:

```json
{
  "error": "ERROR_CODE",
  "message": "Descriptive error message",
  "timestamp": "2026-01-06T10:30:00"
}
```

**Common Error Codes:**
- `EMAIL_ALREADY_EXISTS` - Email already registered (409)
- `INVALID_CREDENTIALS` - Invalid credentials (401)
- `VALIDATION_ERROR` - Validation error (400)
- `INSUFFICIENT_BALANCE` - Insufficient balance (409)
- `RESOURCE_NOT_FOUND` - Resource not found (404)
- `ACCESS_DENIED` - Access denied (403)
- `INSUFFICIENT_STOCK` - Insufficient stock (409)
- `INTERNAL_SERVER_ERROR` - Internal server error (500)

## 🧪 Testing the API

### Using cURL

**Register:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**List products (authenticated):**
```bash
curl -X GET http://localhost:8080/products \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 📝 Important Notes

1. **First Login**: An admin user is created automatically on startup using credentials configured in environment variables.

2. **Production Security**:
   - Change `JWT_SECRET` to a strong key
   - Use HTTPS in production
   - Set `spring.jpa.hibernate.ddl-auto=validate` in production

3. **CORS**: Adjust allowed origins in [CorsConfig.java](src/main/java/com/trustpay/backend/config/CorsConfig.java) as needed.

## 👥 Contributing

Contributions are welcome! Feel free to open issues and pull requests.

## 📄 License

This project is a personal demonstration project.

---

**Built with ❤️ by Miguel Fazio Assuncao**
