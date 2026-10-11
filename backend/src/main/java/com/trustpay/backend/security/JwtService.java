package com.trustpay.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final Key signingKey;
    private final JwtParser parser;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt-secret}") String jwtSecret,
            @Value("${jwt.expiration:86400000}") long expirationMillis
    ) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be a Base64-encoded secret of at least 32 bytes");
        }
        if (expirationMillis < 1000 || expirationMillis > Long.MAX_VALUE - System.currentTimeMillis()) {
            throw new IllegalStateException("JWT_EXPIRATION must be a valid duration of at least 1000 milliseconds");
        }
        try {
            signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JWT_SECRET must be a Base64-encoded secret of at least 32 bytes", ex);
        }
        this.expirationMillis = expirationMillis;
        parser = Jwts.parserBuilder().setSigningKey(signingKey).build();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return parser.parseClaimsJws(token).getBody();
    }

    public String generateToken(String email, Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String email) {
        try {
            Claims claims = extractAllClaims(token);
            return email != null && email.equals(claims.getSubject())
                    && claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
