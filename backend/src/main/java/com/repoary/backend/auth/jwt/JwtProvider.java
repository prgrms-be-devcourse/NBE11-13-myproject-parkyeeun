package com.repoary.backend.auth.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoary.backend.auth.config.JwtProperties;
import com.repoary.backend.user.domain.User;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiredAt = now.plusMillis(jwtProperties.accessTokenExpiration());

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );

        Map<String, Object> payload = Map.of(
                "sub", String.valueOf(user.getId()),
                "githubId", user.getGithubId(),
                "githubLogin", user.getGithubLogin(),
                "iat", now.getEpochSecond(),
                "exp", expiredAt.getEpochSecond()
        );

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String signature = sign(unsignedToken);

        return unsignedToken + "." + signature;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(value);
            return base64UrlEncode(jsonBytes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JWT JSON 직렬화에 실패했습니다.", e);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(keySpec);

            byte[] signatureBytes = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signatureBytes);
        } catch (Exception e) {
            throw new IllegalStateException("JWT 서명 생성에 실패했습니다.", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public Long getUserId(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("유효하지 않은 JWT 형식입니다.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String signature = parts[2];

        String expectedSignature = sign(unsignedToken);
        if (!expectedSignature.equals(signature)) {
            throw new IllegalArgumentException("JWT 서명이 유효하지 않습니다.");
        }

        Map<String, Object> payload = decodePayload(parts[1]);

        Number exp = (Number) payload.get("exp");
        if (exp == null || Instant.now().getEpochSecond() > exp.longValue()) {
            throw new IllegalArgumentException("JWT가 만료되었습니다.");
        }

        return Long.valueOf((String) payload.get("sub"));
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(payloadBytes, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT payload 해석에 실패했습니다.", e);
        }
    }
}