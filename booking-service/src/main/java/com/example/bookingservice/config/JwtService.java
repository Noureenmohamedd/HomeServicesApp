package com.example.bookingservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser validateToken(String token) {
        Claims claims = parseClaims(token);
        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
            throw new JwtException("JWT token has expired");
        }

        Long userId = extractUserId(claims);
        String role = normalizeRole(claims.get("role", String.class));
        if (userId == null || role == null || role.isBlank()) {
            throw new JwtException("JWT token is missing required user claims");
        }

        return new AuthenticatedUser(
                userId,
                role,
                extractDisplayName(claims, userId),
                normalizeProfessionType(claims.get("professionType", String.class))
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long extractUserId(Claims claims) {
        Object userIdClaim = claims.get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim instanceof String userIdText && !userIdText.isBlank()) {
            return Long.parseLong(userIdText);
        }
        String subject = claims.getSubject();
        return subject == null || subject.isBlank() ? null : Long.parseLong(subject);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return normalizedRole.startsWith("ROLE_") ? normalizedRole.substring("ROLE_".length()) : normalizedRole;
    }

    private String normalizeProfessionType(String professionType) {
        if (professionType == null || professionType.isBlank()) {
            return null;
        }
        return professionType.trim().toUpperCase(Locale.ROOT);
    }

    private String extractDisplayName(Claims claims, Long userId) {
        String name = firstPresentClaim(claims, "name", "username", "fullName", "email");
        return name == null || name.isBlank() ? "Customer #" + userId : name;
    }

    private String firstPresentClaim(Claims claims, String... claimNames) {
        for (String claimName : claimNames) {
            Object value = claims.get(claimName);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
