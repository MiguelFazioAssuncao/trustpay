# TrustPay — API de banco digital

API REST de demonstração em Java 21 e Spring Boot 4.0.0, com PostgreSQL, autenticação JWT, contas, transferências, depósitos administrativos, cartões, empréstimos e loja.

## Requisitos e execução

- JDK 21.
- PostgreSQL para executar a aplicação.
- Maven Wrapper incluído; não é necessário instalar Maven. O primeiro build precisa de acesso à internet para baixar as dependências.

Crie o banco:

```sql
CREATE DATABASE trustpay;
```

Na pasta `backend`, prepare as variáveis:

```bash
cd backend
cp .env.example .env
# Edite DATABASE_USERNAME e DATABASE_PASSWORD no arquivo .env.
# Carregue somente arquivos de ambiente que você controla.
set -a
source .env
set +a
export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O Spring Boot não carrega `.env` automaticamente. Exporte as variáveis no terminal, configure a IDE ou forneça-as pelo ambiente do processo. Guarde a chave gerada para reutilizá-la entre reinicializações; trocar a chave invalida os tokens anteriores.

A API fica disponível em `http://localhost:8080`. Os únicos endpoints públicos são `POST /auth/register` e `POST /auth/login`. Todos os outros precisam de:

```http
Authorization: Bearer SEU_TOKEN
```

Com a aplicação ligada, abra o Swagger UI em [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html). A especificação OpenAPI em JSON fica em `http://localhost:8080/v3/api-docs`. No Swagger, clique em **Authorize** e cole o JWT retornado pelo login ou registro; não inclua o prefixo `Bearer`. O Swagger e a especificação são públicos, mas as chamadas a endpoints protegidos continuam exigindo autenticação.

### Primeiro administrador

Antes da primeira inicialização, configure `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_EMAIL` e `ADMIN_PASSWORD`. A senha deve ter 12 a 60 caracteres, maiúscula, minúscula, dígito e caractere especial aceito pela validação, sem espaços e no máximo 72 bytes em UTF-8. Após criar o administrador, desabilite o bootstrap e remova a senha desse ambiente.

O bootstrap não altera usuários existentes nem promove automaticamente uma conta cadastrada. Se já houver um administrador, nenhuma nova conta administrativa é criada. `store@trustpay.com` é reservado para uso interno.

### Build e testes

```bash
cd backend # execute a partir da raiz do repositório
./mvnw clean verify
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Os testes ativam o perfil `test`, usam um banco H2 em memória com compatibilidade PostgreSQL e uma chave JWT exclusiva para testes. A suíte verifica autenticação, validação, concorrência de transferências, cobrança/rollback do checkout, cartões, parcelas e arredondamento. H2 não substitui a validação de migrações e comportamento em PostgreSQL antes de uma implantação.

As alterações passaram em 72 testes com `./mvnw clean verify`. Os três scripts de atualização também foram executados duas vezes em PostgreSQL 17.11, sobre o esquema da versão anterior com dados fictícios. A API atual iniciou nesse banco com o perfil `prod` e validação de esquema; compras com saldo/crédito, leitura de pedidos antigos e pagamento das parcelas foram verificados por HTTP.

### Variáveis de ambiente

| Variável | Uso | Padrão |
| --- | --- | --- |
| `DATABASE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/trustpay` |
| `DATABASE_USERNAME` | Usuário do banco | `postgres` |
| `DATABASE_PASSWORD` | Senha do banco; configure no ambiente | `postgres` em desenvolvimento |
| `JWT_SECRET` | Chave Base64 com ao menos 32 bytes aleatórios | Obrigatória, sem chave padrão |
| `JWT_EXPIRATION` | Duração do token em milissegundos; mínimo 1000 | `86400000` |
| `ADMIN_BOOTSTRAP_ENABLED` | Criar o primeiro administrador | `false` |
| `ADMIN_EMAIL` | E-mail do administrador inicial | Sem padrão |
| `ADMIN_PASSWORD` | Senha do administrador inicial | Sem padrão |
| `CORS_ALLOWED_ORIGINS` | Origens exatas separadas por vírgula | Localhost nas portas 3000, 5173 e 4200; vazio no perfil `prod` |
| `SERVER_PORT` | Porta HTTP | `8080` |
| `LOG_LEVEL` | Nível de log da aplicação | `INFO` |
| `SHOW_SQL` | Exibição de SQL | `false`; habilitado no perfil `dev` |

O perfil `prod` exige banco e credenciais explicitamente configurados e usa `ddl-auto=validate`. Configure as origens CORS que acessarão a API pelo navegador e exponha a aplicação via HTTPS.

### Atualização de um banco existente

As alterações de esquema não são aplicadas automaticamente no perfil `prod`. Com backup e a aplicação parada, revise e execute os scripts PostgreSQL nesta ordem:

```bash
psql "$POSTGRES_CONNECTION_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-core.sql
psql "$POSTGRES_CONNECTION_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-loans.sql
psql "$POSTGRES_CONNECTION_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-checkout.sql
```

`POSTGRES_CONNECTION_URL` deve ser uma conexão do `psql`, como `postgresql://usuario:senha@localhost:5432/trustpay`, e não a URL JDBC. Execute esses comandos a partir da raiz do repositório. Os scripts são destinados a bancos que já têm as tabelas da versão anterior; para um banco novo, o perfil de desenvolvimento cria as tabelas, que devem ser provisionadas e revisadas antes de usar `prod`.

Os scripts verificam inconsistências antes de converter valores, adicionam índices e os novos campos de pedidos/cartões. Não é possível reconstruir quantidades e preços históricos dos pedidos antigos: eles continuam com a lista legada de produtos e `items` vazio. Compras anteriores não são cobradas retroativamente, e o limite usado de cartões anteriores começa em zero. Números completos de cartões antigos também são preservados pelo script e precisam de revisão e saneamento separados. A tabela legada `transictions` mantém o nome para preservar os dados.

Empréstimos antigos podem ter parcelas cuja soma difere do total por causa do arredondamento anterior. O script de empréstimos avisa sobre essas divergências e inclui uma consulta para identificá-las; revise parcelas e dívida das contas antes de retomar os pagamentos. Nenhuma reconciliação financeira é aplicada automaticamente.

## Endpoints

| Método | Caminho | Acesso | Resultado |
| GET | `/swagger-ui/index.html` | Público | Interface interativa |
| GET | `/v3/api-docs` | Público | Especificação OpenAPI em JSON |
| --- | --- | --- | --- |
| POST | `/auth/register` | Público | Token e usuário |
| POST | `/auth/login` | Público | Token e usuário |
| GET | `/users/me` | USER, ADMIN | Conta, saldo e dívida atual |
| POST | `/transactions/transfer` | USER, ADMIN | Transferência; 200 sem corpo |
| GET | `/transactions/my` | USER, ADMIN | Histórico, mais recentes primeiro |
| POST | `/admin/transactions/deposit` | ADMIN | Depósito; 200 sem corpo |
| PATCH | `/admin/users/{userId}/promote` | ADMIN | Promoção; 204, idempotente |
| POST | `/cards` | USER, ADMIN | Cartão mascarado |
| GET | `/cards` | USER, ADMIN | Cartões do titular |
| GET | `/products` | Autenticado | Catálogo |
| POST | `/products` | ADMIN | Produto criado |
| POST | `/store/checkout` | USER, ADMIN | Compra; 200 sem corpo |
| GET | `/store/my-orders` | USER, ADMIN | Histórico de pedidos |
| POST | `/loans/admin/companies` | ADMIN | Parceiro criado |
| GET | `/loans/companies` | USER, ADMIN | Parceiros por nome |
| POST | `/loans/create` | USER, ADMIN | Empréstimo criado |
| GET | `/loans/my` | USER, ADMIN | Empréstimos do titular |
| GET | `/loans/{loanId}/installments` | USER, ADMIN | Parcelas do próprio empréstimo |
| POST | `/loans/pay-installment/{id}` | USER, ADMIN | Pagamento; 200 sem corpo |

### Autenticação

Registro e login usam JSON:

```json
{"email":"usuario@example.com","password":"Trustpay123!"}
```

No registro, a senha deve ter 8 a 60 caracteres, maiúscula, minúscula, dígito e um dos especiais `@#$%^&+=!`, sem espaços. Senhas também respeitam o limite BCrypt de 72 bytes UTF-8. Login aceita as senhas legadas de pelo menos seis caracteres. E-mails são aparados e comparados sem distinguir maiúsculas/minúsculas.

Resposta:

```json
{
  "token": "JWT",
  "message": "Login successful",
  "user": {
    "id": "UUID",
    "email": "usuario@example.com",
    "balance": 100.00,
    "outstandingBalance": 0.00,
    "status": "ACTIVE",
    "role": "USER"
  }
}
```

### Transferência, depósito e produto

`POST /transactions/transfer`:

```json
{"toUserId":"UUID_DESTINATARIO","amount":100.00}
```

`POST /admin/transactions/deposit`:

```json
{"userId":"UUID_USUARIO","amount":500.00}
```

`POST /products` aceita um DTO próprio, sem permitir IDs ou campos de persistência enviados pelo cliente:

```json
{"name":"Produto","price":50.00,"stock":10}
```

Valores positivos vão de `0.01` a `9999999999999.99`. Estoque é inteiro e não negativo. Contas envolvidas em operações financeiras precisam estar ativas.

### Cartões e checkout

`POST /cards` usa `CREDIT_CARD` ou `DEBIT_CARD` (os valores antigos de exemplo `CREDIT`/`DEBIT` eram incorretos). Use somente números de teste nesta demonstração:

```json
{"cardNumber":"4111111111111111","type":"CREDIT_CARD","limit":5000.00}
```

A resposta contém `id`, `cardNumber` mascarado, `type`, `limit`, `usedLimit` e `availableLimit`. Crédito exige limite positivo; débito aceita limite zero e usa o saldo da conta.

`POST /store/checkout`:

```json
{
  "products": [{"productId":"UUID_PRODUTO","quantity":2}],
  "paymentMethod": "ACCOUNT_BALANCE",
  "cardId": null,
  "installments": null
}
```

O carrinho aceita de 1 a 100 entradas. Produtos repetidos têm suas quantidades somadas. `ACCOUNT_BALANCE` não aceita `cardId`; `DEBIT_CARD` e `CREDIT_CARD` exigem um cartão do próprio usuário com o tipo correspondente. `installments` aceita somente `null` ou `1`.

Saldo e débito descontam o comprador e creditam a loja. Crédito preserva o saldo disponível do comprador, aumenta `outstandingBalance` e `usedLimit` e credita a loja no registro local. O pedido só vira `PAID` após o pagamento.

`GET /store/my-orders` mantém os campos anteriores e acrescenta `items` e `paymentMethod`:

```json
[
  {
    "id":"UUID_PEDIDO",
    "userId":"UUID_USUARIO",
    "products":[{"id":"UUID_PRODUTO","name":"Produto","price":50.00,"stock":null}],
    "totalAmount":100.00,
    "status":"PAID",
    "createdAt":"2026-10-10T12:00:00",
    "items":[{"productId":"UUID_PRODUTO","productName":"Produto","quantity":2,"unitPrice":50.00,"subtotal":100.00}],
    "paymentMethod":"ACCOUNT_BALANCE"
  }
]
```

Para pedidos novos, `products` também usa o retrato da compra; `stock` é `null`, pois estoque atual não faz parte do histórico.

### Empréstimos

Os endpoints existentes de criação continuam recebendo parâmetros na URL:

```http
POST /loans/admin/companies?name=Parceiro&email=parceiro@example.com
POST /loans/create?companyId=UUID_PARCEIRO&amount=1000.00&installments=12
```

Nomes e e-mails de parceiros são validados; cadastros duplicados retornam 409. Empréstimos têm juros fixos de 10%, de 1 a 360 parcelas e cada parcela precisa valer pelo menos `0.01`. O valor financiado é arredondado aos centavos; os centavos excedentes são distribuídos entre as primeiras parcelas.

Consulte `/loans/my` para obter o ID do empréstimo e `/loans/{loanId}/installments` para obter os IDs das parcelas:

```json
[
  {
    "id":"UUID_PARCELA",
    "installmentNumber":1,
    "amount":91.67,
    "dueDate":"2026-11-10",
    "status":"PENDING",
    "paidAt":null
  }
]
```

Pague com `POST /loans/pay-installment/UUID_PARCELA`. O pagamento reduz saldo e dívida do titular; a última parcela encerra o empréstimo como `PAID`. Consultar parcelas de outro titular retorna 404; tentar pagá-las retorna 403.

## Erros

```json
{
  "error":"VALIDATION_ERROR",
  "message":"Validation failed",
  "timestamp":"2026-10-10T12:00:00",
  "fields":{"amount":"must be greater than or equal to 0.01"}
}
```

`fields` aparece em erros de validação do corpo. Mensagens são em inglês.

| HTTP | Códigos principais |
| --- | --- |
| 400 | `VALIDATION_ERROR`, `BAD_REQUEST`, `INVALID_CARD` |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | `ACCESS_DENIED`, `UNAUTHORIZED_OPERATION` |
| 404 | `RESOURCE_NOT_FOUND` |
| 405 | `METHOD_NOT_ALLOWED` |
| 409 | `EMAIL_ALREADY_EXISTS`, `INSUFFICIENT_BALANCE`, `INSUFFICIENT_STOCK`, `CONFLICT`, `CONCURRENT_UPDATE` |
| 415 | `UNSUPPORTED_MEDIA_TYPE` |
| 500 | `INTERNAL_SERVER_ERROR` |

## Exemplo com cURL

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"usuario@example.com","password":"Trustpay123!"}'

curl http://localhost:8080/users/me -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/products -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/loans/my -H "Authorization: Bearer $TOKEN"
```

Defina `TOKEN` com o token retornado pelo registro ou login.

## Estrutura

```text
backend/
├── src/main/java/com/trustpay/backend/
│   ├── bootstrap/      # Inicialização de administrador e conta da loja
│   ├── config/         # Segurança e CORS
│   ├── controller/     # Endpoints HTTP
│   ├── dto/            # Entradas validadas e respostas
│   ├── entity/         # Modelo JPA e itens históricos dos pedidos
│   ├── exception/      # Erros de domínio e tratamento HTTP
│   ├── pattern/        # Estratégias de pagamento
│   ├── repository/     # Persistência, consultas e bloqueios
│   ├── security/       # JWT e autenticação
│   ├── services/       # Regras e transações
│   └── validation/     # Valores monetários e estado das contas
├── src/main/resources/ # Perfis e logging
├── src/test/           # Testes unitários e integração HTTP/H2
├── scripts/            # Atualização manual de bancos existentes
└── pom.xml
```

## Limites da demonstração

Cartões e empréstimos são simulações locais: não há integração com adquirentes, análise de crédito, emissão real de cartões ou pagamento da fatura de crédito. O limite é informado pelo próprio usuário, e a dívida de cartões integra `outstandingBalance`. Checkout parcelado não é implementado. Não use cartões reais.

Os endpoints de escrita ainda não têm chaves de idempotência: repetir uma compra, depósito ou transferência após um timeout pode criar outra operação. Históricos retornam listas sem paginação. Antes de uso financeiro real, também são necessários reconciliação/ledger contábil, auditoria, limites operacionais, proteção contra abuso, recuperação de acesso e migrações versionadas.

Projeto pessoal de demonstração de Miguel Fazio Assuncao.
