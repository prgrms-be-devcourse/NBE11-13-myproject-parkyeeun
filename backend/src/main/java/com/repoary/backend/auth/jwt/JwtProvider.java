package com.repoary.backend.auth.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoary.backend.auth.config.JwtProperties;
import com.repoary.backend.auth.exception.AuthErrorCode;
import com.repoary.backend.auth.exception.JwtAuthenticationException;
import com.repoary.backend.auth.exception.JwtSystemException;
import com.repoary.backend.user.domain.User;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
            throw new JwtSystemException(e);
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
            throw new JwtSystemException(e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public Long getUserId(String token) {
        String[] parts = token.split("\\.", -1);

        if (parts.length != 3
                || parts[0].isBlank()
                || parts[1].isBlank()
                || parts[2].isBlank()) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_MALFORMED
            );
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String signature = parts[2];

        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_INVALID_SIGNATURE
            );
        }

        Map<String, Object> payload = decodePayload(parts[1]);

        Object expiration = payload.get("exp");
        if (!(expiration instanceof Number exp)) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_INVALID_PAYLOAD
            );
        }

        if (Instant.now().getEpochSecond() >= exp.longValue()) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_EXPIRED
            );
        }

        try {
            return Long.valueOf(String.valueOf(payload.get("sub")));
        } catch (RuntimeException exception) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_INVALID_PAYLOAD,
                    exception
            );
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        Map<String, Object> payload;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(encodedPayload);
            payload = objectMapper.readValue(
                    payloadBytes,
                    new TypeReference<>() {
                    }
            );
        } catch (Exception e) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_INVALID_PAYLOAD,
                    e
            );
        }

        if (payload == null) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.JWT_INVALID_PAYLOAD
            );
        }
        return payload;
    }
}
