# Backend TrustPay

A documentação atualizada da API, configuração, endpoints, testes e migrações está no [README principal](../README.md).

Depois de iniciar a aplicação, acesse `http://localhost:8080/swagger-ui/index.html`. A especificação OpenAPI em JSON fica em `http://localhost:8080/v3/api-docs`. Use o botão **Authorize** e cole o JWT para testar endpoints protegidos.

Execute os comandos Maven nesta pasta:

```bash
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Para iniciar a aplicação, exporte as variáveis de `.env.example`, incluindo uma chave `JWT_SECRET` Base64 com pelo menos 32 bytes aleatórios. Os testes usam H2 e configuração própria, sem precisar de PostgreSQL externo.
