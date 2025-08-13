package org.oreo.smore.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.auth.jwt.JwtTokenProvider;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {

        // 쿠키에서 accessToken 추출
        String token = extractAccessTokenFromCookies(request);

        if (!StringUtils.hasText(token)) {
            log.warn("❌ WebSocket 연결 실패: accessToken 쿠키가 없음 - IP: {}",
                    request.getRemoteAddress());
            return false;
        }

        try {
            // JWT 토큰 검증 (Access Token)
            if (!jwtTokenProvider.validateToken(token, true)) {
                log.warn("❌ WebSocket 연결 실패: 유효하지 않은 accessToken - IP: {}",
                        request.getRemoteAddress());
                return false;
            }

            // JWT에서 userId 추출 (String → Long 변환)
            String userIdStr = jwtTokenProvider.getUserIdFromToken(token, true);
            Long userId = Long.parseLong(userIdStr);

            // 사용자 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // WebSocket 세션에 사용자 정보 저장
            attributes.put("userId", userId);
            attributes.put("userEmail", user.getEmail());
            attributes.put("user", user);
            attributes.put("token", token);

            log.info("✅ WebSocket 연결 성공 - 사용자: {} (ID: {}), IP: {}",
                    user.getEmail(), userId, request.getRemoteAddress());

            return true;

        } catch (NumberFormatException e) {
            log.error("❌ WebSocket 연결 실패: userId 변환 오류 - IP: {}, 오류: {}",
                    request.getRemoteAddress(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ WebSocket 핸드셰이크 중 오류 발생 - IP: {}, 오류: {}",
                    request.getRemoteAddress(), e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception != null) {
            log.error("❌ WebSocket 핸드셰이크 후 오류 발생 - IP: {}, 오류: {}",
                    request.getRemoteAddress(), exception.getMessage());
        }
    }

    // HTTP 요청의 쿠키에서 accessToken 추출
    private String extractAccessTokenFromCookies(ServerHttpRequest request) {
        String cookieHeader = request.getHeaders().getFirst("Cookie");

        if (!StringUtils.hasText(cookieHeader)) {
            log.debug("Cookie 헤더가 없습니다.");
            return null;
        }

        log.debug("Cookie 헤더: {}", cookieHeader);

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
                    log.debug("✅ accessToken 쿠키 발견: {}...", cookieValue.substring(0, Math.min(20, cookieValue.length())));
                    return cookieValue;
                }
            }
        }

        log.warn("❌ accessToken 쿠키를 찾을 수 없습니다. 사용 가능한 쿠키들:");
        for (String cookie : cookies) {
            String trimmedCookie = cookie.trim();
            int equalIndex = trimmedCookie.indexOf('=');
            if (equalIndex > 0) {
                String cookieName = trimmedCookie.substring(0, equalIndex).trim();
                log.warn("  - {}", cookieName);
            }
        }

        return null;
    }
}