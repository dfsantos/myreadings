package dev.dfsantos.myreadings.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Emite tokens JWT (HS256) com o id do usuário como claim {@code sub}.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    /**
     * Valida assinatura/expiração do token e extrai o id do usuário do claim {@code sub}.
     * Lança {@link JwtException} (ou subclasse, ex: {@code ExpiredJwtException},
     * {@code MalformedJwtException}, {@code SignatureException}) se o token for
     * inválido/expirado/malformado.
     */
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
