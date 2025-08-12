package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠키 파싱 로직 검증 테스트
 */
class CookieParsingTest {

    @Test
    @DisplayName("쿠키에서 accessToken 정상 추출")
    void testExtractAccessTokenFromCookies() {
        // Given
        String cookieHeader = "accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9; refreshToken=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9; otherCookie=value";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

        // When
        String actualToken = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(actualToken).isEqualTo(expectedToken);
    }

    @Test
    @DisplayName("accessToken이 첫 번째 쿠키인 경우")
    void testAccessTokenFirst() {
        // Given
        String cookieHeader = "accessToken=token123; sessionId=sess456";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isEqualTo("token123");
    }

    @Test
    @DisplayName("accessToken이 마지막 쿠키인 경우")
    void testAccessTokenLast() {
        // Given
        String cookieHeader = "sessionId=sess456; userId=user789; accessToken=token123";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isEqualTo("token123");
    }

    @Test
    @DisplayName("공백이 포함된 쿠키 파싱")
    void testCookieWithSpaces() {
        // Given
        String cookieHeader = " accessToken = token123 ; sessionId = sess456 ";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isEqualTo("token123");
    }

    @Test
    @DisplayName("accessToken이 없는 경우")
    void testNoAccessToken() {
        // Given
        String cookieHeader = "sessionId=sess456; userId=user789";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("쿠키 헤더가 비어있는 경우")
    void testEmptyCookieHeader() {
        // Given
        String cookieHeader = "";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("잘못된 형태의 쿠키 (= 없음)")
    void testInvalidCookieFormat() {
        // Given
        String cookieHeader = "invalidCookie; accessToken=token123";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isEqualTo("token123");
    }

    @Test
    @DisplayName("쿠키 값이 비어있는 경우")
    void testEmptyTokenValue() {
        // Given
        String cookieHeader = "accessToken=; sessionId=sess456";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("쿠키 값에 공백만 있는 경우")
    void testWhitespaceOnlyTokenValue() {
        // Given
        String cookieHeader = "accessToken=   ; sessionId=sess456";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("복잡한 실제 쿠키 헤더 테스트")
    void testComplexRealWorldCookie() {
        // Given - 실제 브라우저에서 보낼 수 있는 복잡한 쿠키
        String cookieHeader = "_ga=GA1.1.123456789; _gid=GA1.1.987654321; accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c; refreshToken=another_token; sessionId=abc123";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // When
        String token = extractAccessTokenFromCookieString(cookieHeader);

        // Then
        assertThat(token).isEqualTo(expectedToken);
    }

    /**
     * 실제 HandshakeInterceptor에서 사용하는 쿠키 파싱 로직
     */
    private String extractAccessTokenFromCookieString(String cookieHeader) {
        if (!StringUtils.hasText(cookieHeader)) {
            return null;
        }

        // 쿠키 문자열을 ';'로 분리하여 각 쿠키 파싱
        String[] cookies = cookieHeader.split(";");

        for (String cookie : cookies) {
            String trimmedCookie = cookie.trim(); // 공백 제거

            // '='으로 분리하여 key=value 형태로 파싱
            int equalIndex = trimmedCookie.indexOf('=');
            if (equalIndex > 0) {
                String cookieName = trimmedCookie.substring(0, equalIndex).trim();
                String cookieValue = trimmedCookie.substring(equalIndex + 1).trim();

                // accessToken 쿠키 찾기
                if ("accessToken".equals(cookieName) && StringUtils.hasText(cookieValue)) {
                    return cookieValue;
                }
            }
        }

        return null;
    }
}