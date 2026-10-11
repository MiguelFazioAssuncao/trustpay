package com.trustpay.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void usesConfiguredExpirationAndPreservesClaims() {
        JwtService service = new JwtService(SECRET, 3_600_000);
        String token = service.generateToken("user@example.com", Map.of("role", "USER"));

        assertTrue(service.isTokenValid(token, "user@example.com"));
        assertEquals("USER", service.extractClaim(token, claims -> claims.get("role")));
        assertEquals(3_600_000L, service.extractClaim(token, Claims::getExpiration).getTime()
                - service.extractClaim(token, Claims::getIssuedAt).getTime());
        assertFalse(service.isTokenValid(token, "another@example.com"));
    }

    @Test
    void rejectsMalformedExpiredAndNonExpiringTokens() {
        JwtService service = new JwtService(SECRET, 60_000);
        String expired = signedToken(new Date(System.currentTimeMillis() - 60_000));
        String withoutExpiration = signedToken(null);

        assertFalse(service.isTokenValid("malformed", "user@example.com"));
        assertFalse(service.isTokenValid(expired, "user@example.com"));
        assertFalse(service.isTokenValid(withoutExpiration, "user@example.com"));
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        String otherSecret = Base64.getEncoder().encodeToString(
                "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8));
        String token = new JwtService(otherSecret, 60_000).generateToken("user@example.com", Map.of());

        assertFalse(new JwtService(SECRET, 60_000).isTokenValid(token, "user@example.com"));
    }

    @Test
    void failsAtStartupForMissingOrWeakSecretAndInvalidExpiration() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("c2hvcnQ=", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("!invalid!", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService(SECRET, 0));
        assertThrows(IllegalStateException.class, () -> new JwtService(SECRET, Long.MAX_VALUE));
    }

    private String signedToken(Date expiration) {
        return Jwts.builder().setSubject("user@example.com").setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();
    }
}
