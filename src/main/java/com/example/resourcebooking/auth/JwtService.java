package com.example.resourcebooking.auth;

import com.example.resourcebooking.user.UserAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 characters");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationMinutes * 60;
    }

    public String issue(UserAccount user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("userId", user.getId());
        payload.put("role", user.getRole().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());
        String unsigned = encode(header) + "." + encode(payload);
        return unsigned + "." + sign(unsigned);
    }

    public CurrentUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Malformed token");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Token expired");
            }
            Long userId = ((Number) payload.get("userId")).longValue();
            return new CurrentUser(userId, (String) payload.get("sub"), com.example.resourcebooking.user.Role.valueOf((String) payload.get("role")));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private String encode(Object value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode token", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
