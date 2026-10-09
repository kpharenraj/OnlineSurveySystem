package com.surveysystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration:PT12H}") Duration expiration) {
        if (secret.isBlank()) {
            byte[] bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            this.key = Keys.hmacShaKeyFor(bytes);
            logger.warn("JWT_SECRET is unset; generated an ephemeral key for local development. Set it before deployment.");
        } else {
            byte[] bytes = secret.startsWith("base64:")
                    ? decodeSecret(secret.substring("base64:".length()))
                    : secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes.");
            }
            this.key = Keys.hmacShaKeyFor(bytes);
        }
        this.expiration = expiration;
    }

    public String createToken(long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private byte[] decodeSecret(String secret) {
        try {
            return java.util.Base64.getDecoder().decode(secret);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET is marked base64: but is not valid base64.", exception);
        }
    }
}
