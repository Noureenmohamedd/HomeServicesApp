package com.example.offerservice.service;

import com.example.offerservice.config.JwtClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_SECRET = "change-this-secret-for-production-user-service";

    private final String configuredSecret;

    public JwtService(@Value("${user.service.jwt.secret:}") String configuredSecret) {
        this.configuredSecret = configuredSecret;
    }

    public Optional<JwtClaims> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            return Optional.empty();
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        Long userId = parseLongClaim(payload, "sub");
        if (userId == null) {
            userId = parseLongClaim(payload, "userId");
        }

        String username = parseStringClaim(payload, "username");
        String role = normalizeRole(parseStringClaim(payload, "role"));
        Long expiresAt = parseLongClaim(payload, "exp");

        if (userId == null || username == null || role == null || expiresAt == null) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() >= expiresAt) {
            return Optional.empty();
        }

        return Optional.of(new JwtClaims(userId, username, role));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        String normalizedRole = role.trim().toUpperCase();
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }

        return switch (normalizedRole) {
            case "ADMIN", "CUSTOMER", "PROVIDER" -> normalizedRole;
            default -> null;
        };
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to validate JWT token", exception);
        }
    }

    private String getSecret() {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret;
        }

        String environmentSecret = System.getenv("USER_SERVICE_JWT_SECRET");
        if (environmentSecret != null && !environmentSecret.isBlank()) {
            return environmentSecret;
        }

        return DEFAULT_SECRET;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private String parseStringClaim(String payload, String claimName) {
        String marker = "\"" + claimName + "\":\"";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return null;
        }

        int valueStart = start + marker.length();
        int valueEnd = payload.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return payload.substring(valueStart, valueEnd);
    }

    private Long parseLongClaim(String payload, String claimName) {
        String stringValue = parseStringClaim(payload, claimName);
        if (stringValue != null) {
            return parseLong(stringValue);
        }

        String marker = "\"" + claimName + "\":";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return null;
        }

        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < payload.length() && Character.isDigit(payload.charAt(valueEnd))) {
            valueEnd++;
        }

        return parseLong(payload.substring(valueStart, valueEnd));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
