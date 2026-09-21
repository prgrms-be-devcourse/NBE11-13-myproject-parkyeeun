package com.repoary.backend.auth.jwt;

import com.repoary.backend.auth.config.JwtProperties;
import com.repoary.backend.auth.exception.AuthErrorCode;
import com.repoary.backend.auth.exception.JwtAuthenticationException;
import com.repoary.backend.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtProviderTest {

    private static final String SECRET =
            "test-secret-that-is-long-enough-for-signing";

    @Test
    @DisplayName("생성한 JWT에서 사용자 ID를 읽는다")
    void createAndReadAccessToken() {
        JwtProvider jwtProvider = jwtProvider(60_000);
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getGithubId()).thenReturn(17L);
        when(user.getGithubLogin()).thenReturn("octocat");

        String token = jwtProvider.createAccessToken(user);

        assertThat(jwtProvider.getUserId(token)).isEqualTo(7L);
    }

    @Test
    @DisplayName("형식이 잘못된 JWT를 구분한다")
    void rejectMalformedToken() {
        assertJwtError("not-a-jwt", AuthErrorCode.JWT_MALFORMED);
    }

    @Test
    @DisplayName("서명이 잘못된 JWT를 구분한다")
    void rejectInvalidSignature() {
        JwtProvider jwtProvider = jwtProvider(60_000);
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getGithubId()).thenReturn(17L);
        when(user.getGithubLogin()).thenReturn("octocat");
        String token = jwtProvider.createAccessToken(user);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtProvider.getUserId(tampered))
                .isInstanceOfSatisfying(
                        JwtAuthenticationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(AuthErrorCode.JWT_INVALID_SIGNATURE)
                );
    }

    @Test
    @DisplayName("만료된 JWT를 구분한다")
    void rejectExpiredToken() {
        JwtProvider jwtProvider = jwtProvider(-1_000);
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getGithubId()).thenReturn(17L);
        when(user.getGithubLogin()).thenReturn("octocat");
        String token = jwtProvider.createAccessToken(user);

        assertThatThrownBy(() -> jwtProvider.getUserId(token))
                .isInstanceOfSatisfying(
                        JwtAuthenticationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(AuthErrorCode.JWT_EXPIRED)
                );
    }

    @Test
    @DisplayName("JSON null payload를 잘못된 payload로 처리한다")
    void rejectNullJsonPayload() throws Exception {
        String encodedHeader = base64UrlEncode("{}");
        String encodedPayload = base64UrlEncode("null");
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String token = unsignedToken + "." + sign(unsignedToken);

        assertThatThrownBy(() -> jwtProvider(60_000).getUserId(token))
                .isInstanceOfSatisfying(
                        JwtAuthenticationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(AuthErrorCode.JWT_INVALID_PAYLOAD)
                );
    }

    private void assertJwtError(
            String token,
            AuthErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(() -> jwtProvider(60_000).getUserId(token))
                .isInstanceOfSatisfying(
                        JwtAuthenticationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }

    private JwtProvider jwtProvider(long expiration) {
        return new JwtProvider(new JwtProperties(SECRET, expiration));
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String unsignedToken) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(
                        unsignedToken.getBytes(StandardCharsets.UTF_8)
                ));
    }
}
