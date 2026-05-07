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
        try {
            if (token == null || token.isBlank()) {
                System.out.println("JWT VALIDATION FAILED: token is missing");
                return Optional.empty();
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.out.println("JWT VALIDATION FAILED: malformed token, expected 3 parts but found " + parts.length);
                return Optional.empty();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                System.out.println("JWT VALIDATION FAILED: signature mismatch");
                System.out.println("JWT SignatureException equivalent: token signature does not match configured secret");
                return Optional.empty();
            }

            String payload;
            try {
                payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                System.out.println("JWT PAYLOAD = " + payload);
            } catch (IllegalArgumentException exception) {
                System.out.println("JWT VALIDATION FAILED: malformed Base64Url payload");
                System.out.println("JWT malformed token issue detected");
                exception.printStackTrace();
                return Optional.empty();
            }

            Long userId = parseLongClaim(payload, "sub");
            if (userId == null) {
                userId = parseLongClaim(payload, "userId");
            }

            String username = parseStringClaim(payload, "username");
            String role = normalizeRole(parseStringClaim(payload, "role"));
            String professionType = normalizeProfessionType(parseStringClaim(payload, "professionType"));
            Long expiresAt = parseLongClaim(payload, "exp");

            if (userId == null || username == null || role == null || expiresAt == null) {
                System.out.println("JWT VALIDATION FAILED: required claim missing");
                System.out.println("userId=" + userId + ", username=" + username + ", role=" + role + ", exp=" + expiresAt);
                return Optional.empty();
            }

            long now = Instant.now().getEpochSecond();
            if (now >= expiresAt) {
                System.out.println("JWT VALIDATION FAILED: token expired");
                System.out.println("JWT ExpiredJwtException equivalent: now=" + now + ", exp=" + expiresAt);
                return Optional.empty();
            }

            System.out.println("JWT VALIDATION SUCCESS: userId=" + userId + ", username=" + username
                    + ", role=" + role + ", professionType=" + professionType);
            return Optional.of(new JwtClaims(userId, username, role, professionType));
        } catch (Exception exception) {
            System.out.println("JWT VALIDATION FAILED: unexpected exception");
            exception.printStackTrace();
            return Optional.empty();
        }
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

    private String normalizeProfessionType(String professionType) {
        if (professionType == null || professionType.isBlank()) {
            return null;
        }
        return professionType.trim().toUpperCase();
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
        String systemSecret = System.getProperty("user.service.jwt.secret");
        if (systemSecret != null && !systemSecret.isBlank()) {
            return systemSecret;
        }

        String environmentSecret = System.getenv("USER_SERVICE_JWT_SECRET");
        if (environmentSecret != null && !environmentSecret.isBlank()) {
            return environmentSecret;
        }

        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret;
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
