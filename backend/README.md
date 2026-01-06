# 🏦 TrustPay - Digital Banking API

TrustPay é uma API RESTful completa para um sistema de banking digital, desenvolvida com Spring Boot. O sistema oferece funcionalidades de gestão de contas, transações, cartões, empréstimos e uma loja virtual integrada.

## 📋 Índice

- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Funcionalidades](#funcionalidades)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e Configuração](#instalação-e-configuração)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Executando o Projeto](#executando-o-projeto)
- [Documentação da API](#documentação-da-api)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Padrões de Projeto](#padrões-de-projeto)

## 🚀 Tecnologias Utilizadas

- **Java 21** - Linguagem de programação
- **Spring Boot 4.0.0** - Framework principal
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - Persistência de dados
- **PostgreSQL** - Banco de dados relacional
- **JWT (JSON Web Tokens)** - Autenticação stateless
- **Lombok** - Redução de boilerplate code
- **Maven** - Gerenciamento de dependências
- **Bean Validation** - Validação de dados

## ✨ Funcionalidades

### Autenticação e Autorização
- ✅ Registro de usuários
- ✅ Login com JWT
- ✅ Controle de acesso baseado em roles (USER, ADMIN)

### Gestão de Usuários
- ✅ Visualização de saldo e informações da conta
- ✅ Promoção de usuários para admin (apenas admins)

### Transações
- ✅ Transferências entre usuários
- ✅ Depósitos (apenas admins)
- ✅ Histórico de transações

### Cartões
- ✅ Criação de cartões virtuais
- ✅ Listagem de cartões do usuário
- ✅ Mascaramento de números de cartão nas respostas

### Empréstimos
- ✅ Solicitação de empréstimos com empresas parceiras
- ✅ Sistema de parcelas
- ✅ Pagamento de parcelas
- ✅ Cadastro de empresas parceiras (apenas admins)

### Loja Virtual
- ✅ Listagem de produtos
- ✅ Checkout com diferentes métodos de pagamento
- ✅ Histórico de pedidos
- ✅ Cadastro de produtos (apenas admins)
- ✅ Controle de estoque

## 📦 Pré-requisitos

- Java 21 ou superior
- PostgreSQL 12 ou superior
- Maven 3.6 ou superior
- Git (opcional)

## ⚙️ Instalação e Configuração

### 1. Clone o repositório

```bash
git clone https://github.com/MiguelFazioAssuncao/trustpay.git
cd trustpay/backend
```

### 2. Configure o banco de dados PostgreSQL

Crie um banco de dados no PostgreSQL:

```sql
CREATE DATABASE trustpay;
```

### 3. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto ou configure as variáveis de ambiente no seu sistema:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/trustpay
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=sua_senha

# JWT
JWT_SECRET=sua_chave_secreta_jwt_muito_segura_aqui
JWT_EXPIRATION=86400000

# Admin
ADMIN_EMAIL=admin@trustpay.com
ADMIN_PASSWORD=admin123

# Server
SERVER_PORT=8080
LOG_LEVEL=INFO
```

## 🔐 Variáveis de Ambiente

| Variável | Descrição | Valor Padrão |
|----------|-----------|--------------|
| DATABASE_URL | URL de conexão do PostgreSQL | jdbc:postgresql://localhost:5432/trustpay |
| DATABASE_USERNAME | Usuário do banco de dados | postgres |
| DATABASE_PASSWORD | Senha do banco de dados | postgres |
| JWT_SECRET | Chave secreta para geração de tokens JWT | (valor padrão inseguro) |
| JWT_EXPIRATION | Tempo de expiração do token em ms | 86400000 (24h) |
| ADMIN_EMAIL | Email do usuário admin inicial | admin@trustpay.com |
| ADMIN_PASSWORD | Senha do usuário admin inicial | admin123 |
| SERVER_PORT | Porta do servidor | 8080 |
| LOG_LEVEL | Nível de log da aplicação | INFO |

## 🏃 Executando o Projeto

### Modo Development

```bash
# Compilar e executar
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou usar o Maven instalado
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Modo Production

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Build do projeto

```bash
# Gerar o JAR
./mvnw clean package

# Executar o JAR gerado
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

A aplicação estará disponível em: `http://localhost:8080`

## 📚 Documentação da API

### Base URL
```
http://localhost:8080
```

### Autenticação

Todos os endpoints, exceto `/auth/**`, requerem autenticação via JWT Bearer Token.

**Header de Autenticação:**
```
Authorization: Bearer {seu_token_jwt}
```

---

## 🔑 Endpoints de Autenticação

### Registrar Usuário
```http
POST /auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "senha123"
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
  "password": "senha123"
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

## 💳 Endpoints de Cartões

**Requer:** Role `USER`

### Criar Cartão
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

### Listar Meus Cartões
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

## 💰 Endpoints de Transações

**Requer:** Role `USER` ou `ADMIN`

### Transferir Dinheiro
```http
POST /transactions/transfer
```

**Request Body:**
```json
{
  "toUserId": "uuid-do-destinatario",
  "amount": 100.00
}
```

**Response:** `200 OK`

### Listar Minhas Transações
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

## 🏪 Endpoints de Produtos

### Listar Produtos
```http
GET /products
```

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "name": "Produto Exemplo",
    "price": 50.00,
    "stock": 100
  }
]
```

### Criar Produto
**Requer:** Role `ADMIN`

```http
POST /products
```

**Request Body:**
```json
{
  "name": "Novo Produto",
  "price": 75.00,
  "stock": 50
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "name": "Novo Produto",
  "price": 75.00,
  "stock": 50
}
```

---

## 🛒 Endpoints da Loja

**Requer:** Role `USER` ou `ADMIN`

### Fazer Checkout
```http
POST /store/checkout
```

**Request Body:**
```json
{
  "products": [
    {
      "productId": "uuid-do-produto",
      "quantity": 2
    }
  ],
  "paymentMethod": "ACCOUNT_BALANCE",
  "cardId": null,
  "installments": null
}
```

**Response:** `200 OK`

### Listar Meus Pedidos
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
        "name": "Produto",
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

## 💸 Endpoints de Empréstimos

### Criar Empresa Parceira
**Requer:** Role `ADMIN`

```http
POST /loans/admin/companies?name=Empresa&email=empresa@example.com
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "name": "Empresa",
  "email": "empresa@example.com"
}
```

### Solicitar Empréstimo
```http
POST /loans/create?companyId=uuid&amount=1000&installments=12
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "userId": "uuid",
  "companyName": "Empresa",
  "principalAmount": 1000.00,
  "totalAmount": 1100.00,
  "totalInstallments": 12,
  "status": "ACTIVE",
  "createdAt": "2026-01-06T10:30:00"
}
```

### Pagar Parcela
```http
POST /loans/pay-installment/{installmentId}
```

**Response:** `200 OK`

---

## 👑 Endpoints de Administração

### Promover Usuário para Admin
**Requer:** Role `ADMIN`

```http
PATCH /admin/users/{userId}/promote
```

**Response:** `204 No Content`

### Depositar Dinheiro em Conta
**Requer:** Role `ADMIN`

```http
POST /admin/transactions/deposit
```

**Request Body:**
```json
{
  "userId": "uuid-do-usuario",
  "amount": 500.00
}
```

**Response:** `200 OK`

---

## 📁 Estrutura do Projeto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/trustpay/backend/
│   │   │   ├── bootstrap/          # Inicialização de dados
│   │   │   ├── config/             # Configurações (Security, CORS)
│   │   │   ├── controller/         # Endpoints REST
│   │   │   │   └── admin/          # Endpoints administrativos
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   ├── auth/           # DTOs de autenticação
│   │   │   │   ├── admin/          # DTOs administrativos
│   │   │   │   └── response/       # DTOs de resposta
│   │   │   ├── entity/             # Entidades JPA
│   │   │   ├── enums/              # Enumerações
│   │   │   ├── exception/          # Exceções customizadas
│   │   │   │   └── handler/        # Global Exception Handler
│   │   │   ├── pattern/            # Padrões de projeto (Strategy)
│   │   │   ├── repository/         # Repositórios JPA
│   │   │   ├── scheduler/          # Tarefas agendadas
│   │   │   ├── security/           # JWT e configurações de segurança
│   │   │   └── services/           # Lógica de negócio
│   │   │       ├── admin/          # Serviços administrativos
│   │   │       └── impl/           # Implementações de serviços
│   │   └── resources/
│   │       ├── application.properties       # Configurações principais
│   │       ├── application-dev.properties   # Perfil desenvolvimento
│   │       └── application-prod.properties  # Perfil produção
│   └── test/
└── pom.xml
```

## 🎯 Padrões de Projeto

### Strategy Pattern
Implementado para diferentes métodos de pagamento:
- `AccountBalancePaymentStrategy` - Pagamento com saldo da conta
- `CardPaymentStrategy` - Pagamento com cartão

### DTO Pattern
Separação clara entre entidades de domínio e objetos de transferência:
- DTOs de Request para entrada de dados
- DTOs de Response para saída, protegendo informações sensíveis

### Repository Pattern
Abstração da camada de acesso a dados com Spring Data JPA

### Service Layer Pattern
Camada de serviço separada para lógica de negócio

## 🔒 Segurança

- **JWT Authentication**: Tokens stateless para autenticação
- **BCrypt**: Hash de senhas
- **Role-Based Access Control**: Controle de acesso baseado em roles
- **CORS**: Configurado para permitir apenas origens específicas
- **Input Validation**: Validação de entrada com Bean Validation
- **Exception Handling**: Tratamento global de exceções

## 🐛 Tratamento de Erros

A API retorna respostas de erro padronizadas:

```json
{
  "error": "ERROR_CODE",
  "message": "Mensagem descritiva do erro",
  "timestamp": "2026-01-06T10:30:00"
}
```

**Códigos de Erro Comuns:**
- `EMAIL_ALREADY_EXISTS` - Email já cadastrado (409)
- `INVALID_CREDENTIALS` - Credenciais inválidas (401)
- `VALIDATION_ERROR` - Erro de validação (400)
- `INSUFFICIENT_BALANCE` - Saldo insuficiente (409)
- `RESOURCE_NOT_FOUND` - Recurso não encontrado (404)
- `ACCESS_DENIED` - Acesso negado (403)
- `INSUFFICIENT_STOCK` - Estoque insuficiente (409)
- `INTERNAL_SERVER_ERROR` - Erro interno do servidor (500)

## 🧪 Testando a API

### Usando cURL

**Registrar:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"senha123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"senha123"}'
```

**Listar produtos (com autenticação):**
```bash
curl -X GET http://localhost:8080/products \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

## 📝 Notas Importantes

1. **Primeiro Login**: Um usuário admin é criado automaticamente na inicialização com as credenciais configuradas nas variáveis de ambiente.

2. **Segurança em Produção**: 
   - Altere o `JWT_SECRET` para uma chave forte
   - Use HTTPS em produção
   - Configure `spring.jpa.hibernate.ddl-auto=validate` em produção

3. **CORS**: Ajuste as origens permitidas em [CorsConfig.java](src/main/java/com/trustpay/backend/config/CorsConfig.java) conforme necessário.

## 👥 Contribuindo

Contribuições são bem-vindas! Sinta-se à vontade para abrir issues e pull requests.

## 📄 Licença

Este projeto é um projeto pessoal de demonstração.

---

**Desenvolvido com ❤️ por Miguel Fazio Assunção**
