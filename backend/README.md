# TrustPay Backend

The current API documentation, configuration, endpoints, tests, and migrations are covered in the [main README](../README.md).

After starting the application, open `http://localhost:8080/swagger-ui/index.html`. The OpenAPI JSON specification is available at `http://localhost:8080/v3/api-docs`. Use the **Authorize** button and paste the JWT to test protected endpoints.

Run Maven commands in this directory:

```bash
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

To start the application, export the variables from `.env.example`, including a Base64 `JWT_SECRET` key with at least 32 random bytes. Tests use H2 and their own configuration, without requiring an external PostgreSQL instance.
