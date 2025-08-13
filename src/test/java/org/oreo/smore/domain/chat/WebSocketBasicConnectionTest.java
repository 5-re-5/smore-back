package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.oreo.smore.domain.auth.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket STOMP 기본 연결 테스트 - 트랜잭션 문제 해결
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.servlet.context-path=/",
                "logging.level.org.springframework.web.socket=DEBUG",
                "logging.level.org.oreo.smore=DEBUG"
        }
)
@ActiveProfiles("test")
@Import(WebSocketTestConfig.class)
class WebSocketBasicConnectionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private WebSocketStompClient stompClient;
    private String validJwt;
    private User testUser;

    @BeforeEach
    @Transactional
    @Commit // 🔥 강제로 커밋하여 다른 트랜잭션에서도 사용자 조회 가능하게 함
    void setUp() {
        // 기존 테스트 데이터 정리 (혹시 모를 충돌 방지)
        userRepository.deleteAll();

        // WebSocket STOMP 클라이언트 설정
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // 테스트 사용자 생성 및 저장
        testUser = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .name("테스트")
                .profileUrl("https://example.com/profile.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(60)
                .level("초급")
                .build();

        testUser = userRepository.save(testUser); // 저장된 엔티티로 업데이트
        userRepository.flush(); // 강제로 DB에 반영

        // 유효한 JWT 토큰 생성
        validJwt = jwtTokenProvider.createAccessToken(testUser.getUserId().toString());

        System.out.println("=== WebSocket Test Setup ===");
        System.out.println("Test Port: " + port);
        System.out.println("Test User ID: " + testUser.getUserId());
        System.out.println("JWT Token: " + validJwt.substring(0, 50) + "...");

        // 🔥 사용자가 실제로 저장되었는지 확인
        User foundUser = userRepository.findById(testUser.getUserId()).orElse(null);
        System.out.println("✅ 저장된 사용자 확인: " + (foundUser != null ? foundUser.getUserId() : "NOT FOUND"));
    }

    @Test
    @DisplayName("1. WebSocket 연결 성공 테스트")
    void testWebSocketConnection() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        System.out.println("🔗 연결 시도 URL: " + url);

        BlockingQueue<String> connectionResult = new LinkedBlockingQueue<>();

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add("Cookie", "accessToken=" + validJwt);

        // When
        StompSessionHandler sessionHandler = new TestStompSessionHandler(connectionResult);

        try {
            StompSession session = stompClient.connectAsync(url, httpHeaders, sessionHandler)
                    .get(10, TimeUnit.SECONDS);

            // Then
            assertTrue(session.isConnected(), "WebSocket 세션이 연결되어야 합니다");

            String result = connectionResult.poll(5, TimeUnit.SECONDS);
            assertEquals("CONNECTED", result, "연결 성공 메시지를 받아야 합니다");

            System.out.println("✅ WebSocket 연결 성공!");
            session.disconnect();

        } catch (Exception e) {
            System.err.println("❌ WebSocket 연결 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("2. Ping-Pong 메시지 테스트")
    void testPingPongMessage() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<Map<String, Object>> messageQueue = new LinkedBlockingQueue<>();

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add("Cookie", "accessToken=" + validJwt);

        StompSessionHandler sessionHandler = new TestStompSessionHandler(new LinkedBlockingQueue<>());
        StompSession session = stompClient.connectAsync(url, httpHeaders, sessionHandler)
                .get(10, TimeUnit.SECONDS);

        // 응답 메시지 구독
        session.subscribe("/topic/test/pong", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((Map<String, Object>) payload);
            }
        });

        // When - Ping 메시지 전송
        Map<String, Object> pingMessage = Map.of("message", "ping");
        session.send("/app/test/ping", pingMessage);

        // Then - Pong 메시지 수신 확인
        Map<String, Object> response = messageQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(response, "Pong 응답을 받아야 합니다");
        assertThat(response.get("message")).isEqualTo("pong");
        assertThat(response.get("originalMessage")).isEqualTo("ping");

        session.disconnect();
    }

    @Test
    @DisplayName("3. 토큰 없는 연결 실패 테스트")
    void testConnectionWithoutToken() {
        String url = "ws://localhost:" + port + "/ws/chat";

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        // 토큰 없이 연결 시도

        assertThrows(Exception.class, () -> {
            BlockingQueue<String> connectionResult = new LinkedBlockingQueue<>();
            StompSessionHandler sessionHandler = new TestStompSessionHandler(connectionResult);
            stompClient.connectAsync(url, httpHeaders, sessionHandler)
                    .get(5, TimeUnit.SECONDS);
        }, "토큰 없이는 연결이 실패해야 합니다");

        System.out.println("✅ 토큰 없는 연결 실패 테스트 통과");
    }

    @Test
    @DisplayName("4. 잘못된 토큰 연결 실패 테스트")
    void testConnectionWithInvalidToken() {
        String url = "ws://localhost:" + port + "/ws/chat";
        String invalidToken = "invalid.jwt.token.here";

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add("Cookie", "accessToken=" + invalidToken);

        assertThrows(Exception.class, () -> {
            BlockingQueue<String> connectionResult = new LinkedBlockingQueue<>();
            StompSessionHandler sessionHandler = new TestStompSessionHandler(connectionResult);
            stompClient.connectAsync(url, httpHeaders, sessionHandler)
                    .get(5, TimeUnit.SECONDS);
        }, "잘못된 토큰으로는 연결이 실패해야 합니다");

        System.out.println("✅ 잘못된 토큰 연결 실패 테스트 통과");
    }

    @Test
    @DisplayName("5. 다중 클라이언트 연결 테스트")
    void testMultipleClientConnections() throws Exception {
        String url = "ws://localhost:" + port + "/ws/chat";
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add("Cookie", "accessToken=" + validJwt);

        // When - 2개의 클라이언트 동시 연결
        StompSession session1 = stompClient.connectAsync(url, httpHeaders,
                        new TestStompSessionHandler(new LinkedBlockingQueue<>()))
                .get(10, TimeUnit.SECONDS);

        StompSession session2 = stompClient.connectAsync(url, httpHeaders,
                        new TestStompSessionHandler(new LinkedBlockingQueue<>()))
                .get(10, TimeUnit.SECONDS);

        // Then
        assertTrue(session1.isConnected(), "첫 번째 세션이 연결되어야 합니다");
        assertTrue(session2.isConnected(), "두 번째 세션이 연결되어야 합니다");
        assertNotEquals(session1.getSessionId(), session2.getSessionId(),
                "각 세션은 고유한 ID를 가져야 합니다");

        System.out.println("✅ 다중 클라이언트 연결 테스트 통과");

        session1.disconnect();
        session2.disconnect();
    }

    /**
     * 향상된 테스트용 STOMP 세션 핸들러
     */
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        private final BlockingQueue<String> connectionResult;

        public TestStompSessionHandler(BlockingQueue<String> connectionResult) {
            this.connectionResult = connectionResult;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("✅ WebSocket 연결 성공");
            System.out.println("  - Session ID: " + session.getSessionId());
            System.out.println("  - Connected Headers: " + connectedHeaders);
            connectionResult.offer("CONNECTED");
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("❌ WebSocket STOMP 예외 발생");
            System.err.println("  - Command: " + command);
            System.err.println("  - Headers: " + headers);
            System.err.println("  - Exception: " + exception.getMessage());
            exception.printStackTrace();
            connectionResult.offer("ERROR: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("❌ WebSocket 전송 오류");
            System.err.println("  - Session: " + (session != null ? session.getSessionId() : "null"));
            System.err.println("  - Exception Type: " + exception.getClass().getSimpleName());
            System.err.println("  - Exception Message: " + exception.getMessage());
            exception.printStackTrace();
            connectionResult.offer("TRANSPORT_ERROR: " + exception.getMessage());
        }
    }
}