package com.example.userserviceejb.service;

import com.example.userserviceejb.entity.ProfessionType;
import com.example.userserviceejb.entity.User;
import com.example.userserviceejb.entity.UserRole;
import jakarta.ejb.Stateless;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Stateless
public class JwtServiceBean {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";
    private static final long EXPIRATION_SECONDS = 2 * 60 * 60;
    private static final String DEFAULT_SECRET = "change-this-secret-for-production-user-service";

    public String generateToken(User user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + EXPIRATION_SECONDS;

        String header = "{\"alg\":\"HS256\",\"typ\":\"" + TOKEN_TYPE + "\"}";
        ProfessionType professionType = user.getProfessionType() == null
                ? ProfessionType.OTHER
                : user.getProfessionType();
        String payload = "{"
                + "\"sub\":\"" + user.getId() + "\","
                + "\"username\":\"" + escapeJson(user.getUsername()) + "\","
                + "\"role\":\"" + user.getRole().name() + "\","
                + "\"professionType\":\"" + professionType.name() + "\","
                + "\"iat\":" + issuedAt + ","
                + "\"exp\":" + expiresAt
                + "}";

        String unsignedToken = base64UrlEncode(header) + "." + base64UrlEncode(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public Optional<TokenClaims> validateToken(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
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

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Long userId = parseLongClaim(payload, "sub");
        String username = parseStringClaim(payload, "username");
        String roleValue = parseStringClaim(payload, "role");
        String professionTypeValue = parseStringClaim(payload, "professionType");
        Long expiresAt = parseLongClaim(payload, "exp");

        if (userId == null || username == null || roleValue == null || expiresAt == null) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() >= expiresAt) {
            return Optional.empty();
        }

        try {
            ProfessionType professionType = professionTypeValue == null
                    ? ProfessionType.OTHER
                    : ProfessionType.valueOf(professionTypeValue);
            return Optional.of(new TokenClaims(userId, username, UserRole.valueOf(roleValue), professionType));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            return null;
        }

        String trimmedHeader = authorizationHeader.trim();
        if (trimmedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmedHeader.substring(7).trim();
        }
        return trimmedHeader;
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT token", exception);
        }
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
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

    private String getSecret() {
        String secret = System.getProperty("user.service.jwt.secret");
        if (secret == null || secret.trim().isEmpty()) {
            secret = System.getenv("USER_SERVICE_JWT_SECRET");
        }
        if (secret == null || secret.trim().isEmpty()) {
            return DEFAULT_SECRET;
        }
        return secret;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    public static class TokenClaims {

        private final Long userId;
        private final String username;
        private final UserRole role;
        private final ProfessionType professionType;

        public TokenClaims(Long userId, String username, UserRole role, ProfessionType professionType) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.professionType = professionType;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public UserRole getRole() {
            return role;
        }

        public ProfessionType getProfessionType() {
            return professionType;
        }
    }
}
