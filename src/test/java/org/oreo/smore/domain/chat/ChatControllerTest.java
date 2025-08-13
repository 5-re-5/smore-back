package org.oreo.smore.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.auth.jwt.JwtTokenProvider;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.servlet.context-path=/",
                "logging.level.org.springframework.web.socket=DEBUG",
                "logging.level.org.oreo.smore.domain.chat=DEBUG"
        }
)
@ActiveProfiles("test")
@Import(WebSocketTestConfig.class)
class ChatControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private String validJwt;
    private User testUser;

    @BeforeEach
    @Transactional
    @Commit
    void setUp() {
        // 기존 데이터 정리
        chatMessageRepository.deleteAll();
        userRepository.deleteAll();

        // WebSocket 클라이언트 설정
        stompClient = createStompClient();

        // 테스트 사용자 생성
        testUser = User.builder()
                .email("chat-test@example.com")
                .nickname("채팅테스터")
                .name("채팅 테스트")
                .profileUrl("https://example.com/profile.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(60)
                .level("초급")
                .build();

        testUser = userRepository.saveAndFlush(testUser);
        validJwt = jwtTokenProvider.createAccessToken(testUser.getUserId().toString());

        System.out.println("=== ChatController Test Setup ===");
        System.out.println("Test Port: " + port);
        System.out.println("Test User: " + testUser.getNickname() + " (ID: " + testUser.getUserId() + ")");
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());

        // Jackson 메시지 컨버터 설정 (JSR310 모듈 포함)
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        ObjectMapper testObjectMapper = new ObjectMapper();
        testObjectMapper.registerModule(new JavaTimeModule());
        testObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        messageConverter.setObjectMapper(testObjectMapper);

        client.setMessageConverter(messageConverter);
        return client;
    }

    @Test
    @DisplayName("✅ 1. 기본 채팅 메시지 전송 및 브로드캐스트 테스트 (API 문서 준수)")
    void testBasicChatMessage() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", "accessToken=" + validJwt);

        StompSession session = stompClient.connectAsync(url, headers, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // ✅ API 문서: /topic/chat/broadcast 구독
        session.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageDTO.Broadcast) payload);
            }
        });

        // When - ✅ API 문서: /app/chat/send로 채팅 메시지 전송
        ChatMessageDTO.Request chatRequest = ChatMessageDTO.Request.builder()
                .roomId(1L)
                .content("안녕하세요! 첫 번째 메시지입니다.")
                .messageType(MessageType.CHAT)
                .build();

        session.send("/app/chat/send", chatRequest);

        // Then - ✅ API 문서에 맞는 브로드캐스트 메시지 검증
        ChatMessageDTO.Broadcast receivedMessage = messageQueue.poll(10, TimeUnit.SECONDS);

        assertNotNull(receivedMessage, "브로드캐스트 메시지를 받아야 합니다");
        assertThat(receivedMessage.getMessageId()).isNotNull(); // ✅ API 문서: messageId 추가
        assertThat(receivedMessage.getRoomId()).isEqualTo(1L);
        assertThat(receivedMessage.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(receivedMessage.getNickname()).isEqualTo(testUser.getNickname());
        assertThat(receivedMessage.getContent()).isEqualTo("안녕하세요! 첫 번째 메시지입니다.");
        assertThat(receivedMessage.getMessageType()).isEqualTo(MessageType.CHAT);
        assertThat(receivedMessage.getBroadcastType()).isEqualTo("NEW_MESSAGE"); // ✅ API 문서
        assertNotNull(receivedMessage.getTimestamp());
        assertNotNull(receivedMessage.getMetadata()); // ✅ API 문서: metadata 포함

        System.out.println("✅ 기본 채팅 메시지 테스트 통과 (API 문서 준수)");
        session.disconnect();
    }

    @Test
    @DisplayName("✅ 2. 사용자 입장 알림 테스트 (API 문서 준수)")
    void testUserJoinMessage() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", "accessToken=" + validJwt);

        StompSession session = stompClient.connectAsync(url, headers, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageDTO.Broadcast) payload);
            }
        });

        // When - ✅ API 문서: /app/chat/join으로 사용자 입장
        ChatMessageDTO.Request joinRequest = ChatMessageDTO.Request.builder()
                .roomId(1L)
                .build();

        session.send("/app/chat/join", joinRequest);

        // Then - ✅ API 문서에 맞는 입장 알림 검증
        ChatMessageDTO.Broadcast joinMessage = messageQueue.poll(10, TimeUnit.SECONDS);

        assertNotNull(joinMessage);
        assertThat(joinMessage.getMessageId()).isNotNull(); // ✅ API 문서: messageId 추가
        assertThat(joinMessage.getRoomId()).isEqualTo(1L);
        assertThat(joinMessage.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(joinMessage.getNickname()).isEqualTo(testUser.getNickname());
        assertThat(joinMessage.getContent()).contains("입장하셨습니다");
        assertThat(joinMessage.getMessageType()).isEqualTo(MessageType.USER_JOIN);
        assertThat(joinMessage.getBroadcastType()).isEqualTo("USER_JOIN"); // ✅ API 문서

        System.out.println("✅ 사용자 입장 알림 테스트 통과 (API 문서 준수)");
        session.disconnect();
    }

    @Test
    @DisplayName("✅ 3. 사용자 퇴장 알림 테스트 (API 문서 준수)")
    void testUserLeaveMessage() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", "accessToken=" + validJwt);

        StompSession session = stompClient.connectAsync(url, headers, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageDTO.Broadcast) payload);
            }
        });

        // When - ✅ API 문서: /app/chat/leave로 사용자 퇴장
        ChatMessageDTO.Request leaveRequest = ChatMessageDTO.Request.builder()
                .roomId(1L)
                .build();

        session.send("/app/chat/leave", leaveRequest);

        // Then - ✅ API 문서에 맞는 퇴장 알림 검증
        ChatMessageDTO.Broadcast leaveMessage = messageQueue.poll(10, TimeUnit.SECONDS);

        assertNotNull(leaveMessage);
        assertThat(leaveMessage.getMessageId()).isNotNull(); // ✅ API 문서: messageId 추가
        assertThat(leaveMessage.getRoomId()).isEqualTo(1L);
        assertThat(leaveMessage.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(leaveMessage.getNickname()).isEqualTo(testUser.getNickname());
        assertThat(leaveMessage.getContent()).contains("퇴장하셨습니다");
        assertThat(leaveMessage.getMessageType()).isEqualTo(MessageType.USER_LEAVE);
        assertThat(leaveMessage.getBroadcastType()).isEqualTo("USER_LEAVE"); // ✅ API 문서

        System.out.println("✅ 사용자 퇴장 알림 테스트 통과 (API 문서 준수)");
        session.disconnect();
    }

    @Test
    @DisplayName("✅ 4. 다중 사용자 메시지 교환 테스트 (API 문서 준수)")
    void testMultiUserMessageExchange() throws Exception {
        // Given - 두 번째 사용자 생성
        User secondUser = User.builder()
                .email("chat-test2@example.com")
                .nickname("채팅테스터2")
                .name("채팅 테스트2")
                .profileUrl("https://example.com/profile2.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(60)
                .level("중급")
                .build();
        secondUser = userRepository.saveAndFlush(secondUser);
        String secondJwt = jwtTokenProvider.createAccessToken(secondUser.getUserId().toString());

        String url = "ws://localhost:" + port + "/ws/chat";

        // 중복 메시지 필터링을 위한 Set
        Set<String> receivedMessages = ConcurrentHashMap.newKeySet();
        BlockingQueue<ChatMessageDTO.Broadcast> firstUserQueue = new LinkedBlockingQueue<>();

        // 첫 번째 사용자 연결
        WebSocketHttpHeaders headers1 = new WebSocketHttpHeaders();
        headers1.add("Cookie", "accessToken=" + validJwt);
        StompSession session1 = stompClient.connectAsync(url, headers1, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 두 번째 사용자용 클라이언트 생성
        WebSocketStompClient stompClient2 = createStompClient();
        WebSocketHttpHeaders headers2 = new WebSocketHttpHeaders();
        headers2.add("Cookie", "accessToken=" + secondJwt);
        StompSession session2 = stompClient2.connectAsync(url, headers2, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 첫 번째 사용자만 메시지 수신 (중복 방지)
        session1.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessageDTO.Broadcast message = (ChatMessageDTO.Broadcast) payload;
                String messageKey = message.getUserId() + ":" + message.getContent();
                if (receivedMessages.add(messageKey)) { // 중복이 아닌 경우만 추가
                    firstUserQueue.offer(message);
                }
            }
        });

        // 두 번째 사용자는 구독만 (메시지 수집하지 않음)
        session2.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // 메시지 수신만 하고 수집하지 않음
            }
        });

        // 연결 안정화 대기
        Thread.sleep(500);

        // When - 메시지 교환
        // 첫 번째 사용자가 메시지 전송
        ChatMessageDTO.Request message1 = ChatMessageDTO.Request.builder()
                .roomId(1L)
                .content("첫 번째 사용자 메시지")
                .build();
        session1.send("/app/chat/send", message1);

        Thread.sleep(1000); // 메시지 처리 대기

        // 두 번째 사용자가 메시지 전송
        ChatMessageDTO.Request message2 = ChatMessageDTO.Request.builder()
                .roomId(1L)
                .content("두 번째 사용자 메시지")
                .build();
        session2.send("/app/chat/send", message2);

        Thread.sleep(1000); // 메시지 처리 대기

        // Then - ✅ API 문서에 맞는 메시지 수신 확인
        ChatMessageDTO.Broadcast firstMessage = firstUserQueue.poll(5, TimeUnit.SECONDS);
        ChatMessageDTO.Broadcast secondMessage = firstUserQueue.poll(5, TimeUnit.SECONDS);

        assertNotNull(firstMessage, "첫 번째 메시지를 받아야 합니다");
        assertNotNull(secondMessage, "두 번째 메시지를 받아야 합니다");

        // ✅ API 문서: messageId 필드 검증
        assertThat(firstMessage.getMessageId()).isNotNull();
        assertThat(secondMessage.getMessageId()).isNotNull();

        // 메시지 순서는 보장되지 않을 수 있으므로 내용으로 구분
        if (firstMessage.getUserId().equals(testUser.getUserId())) {
            assertThat(firstMessage.getNickname()).isEqualTo(testUser.getNickname());
            assertThat(firstMessage.getContent()).isEqualTo("첫 번째 사용자 메시지");
            assertThat(firstMessage.getBroadcastType()).isEqualTo("NEW_MESSAGE"); // ✅ API 문서

            assertThat(secondMessage.getUserId()).isEqualTo(secondUser.getUserId());
            assertThat(secondMessage.getNickname()).isEqualTo(secondUser.getNickname());
            assertThat(secondMessage.getContent()).isEqualTo("두 번째 사용자 메시지");
            assertThat(secondMessage.getBroadcastType()).isEqualTo("NEW_MESSAGE"); // ✅ API 문서
        } else {
            assertThat(firstMessage.getUserId()).isEqualTo(secondUser.getUserId());
            assertThat(firstMessage.getNickname()).isEqualTo(secondUser.getNickname());
            assertThat(firstMessage.getContent()).isEqualTo("두 번째 사용자 메시지");

            assertThat(secondMessage.getUserId()).isEqualTo(testUser.getUserId());
            assertThat(secondMessage.getNickname()).isEqualTo(testUser.getNickname());
            assertThat(secondMessage.getContent()).isEqualTo("첫 번째 사용자 메시지");
        }

        System.out.println("✅ 다중 사용자 메시지 교환 테스트 통과 (API 문서 준수)");

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("✅ 5. 연속 메시지 전송 테스트 (API 문서 준수)")
    void testConsecutiveMessages() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();
        Set<String> receivedContents = ConcurrentHashMap.newKeySet();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", "accessToken=" + validJwt);

        StompSession session = stompClient.connectAsync(url, headers, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessageDTO.Broadcast message = (ChatMessageDTO.Broadcast) payload;
                if (receivedContents.add(message.getContent())) { // 중복이 아닌 경우만 추가
                    messageQueue.offer(message);
                }
            }
        });

        // 연결 안정화 대기
        Thread.sleep(500);

        // When - 연속으로 3개 메시지 전송 (간격을 두고)
        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(1L)
                    .content("연속 메시지 " + i)
                    .build();
            session.send("/app/chat/send", request);
            Thread.sleep(200); // 200ms 간격으로 전송
        }

        // Then - ✅ API 문서에 맞는 3개 메시지 모두 수신 확인
        Set<String> expectedMessages = Set.of("연속 메시지 1", "연속 메시지 2", "연속 메시지 3");
        Set<String> actualMessages = ConcurrentHashMap.newKeySet();

        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Broadcast message = messageQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(message, "메시지 " + i + "을 받아야 합니다");
            assertThat(message.getUserId()).isEqualTo(testUser.getUserId());
            assertThat(message.getMessageId()).isNotNull(); // ✅ API 문서: messageId 검증
            assertThat(message.getBroadcastType()).isEqualTo("NEW_MESSAGE"); // ✅ API 문서
            actualMessages.add(message.getContent());
        }

        // 모든 예상 메시지가 수신되었는지 확인
        assertThat(actualMessages).containsExactlyInAnyOrderElementsOf(expectedMessages);

        System.out.println("✅ 연속 메시지 전송 테스트 통과 (API 문서 준수)");
        session.disconnect();
    }

    @Test
    @DisplayName("✅ 6. 빈 메시지 전송 시 유효성 실패 테스트 (API 문서 준수)")
    void testSendEmptyMessage_ValidationError() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/chat";
        String validJwt = jwtTokenProvider.createAccessToken(testUser.getUserId().toString());

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", "accessToken=" + validJwt);

        StompSession session = stompClient
                .connectAsync(url, headers, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 브로드캐스트 구독
        BlockingQueue<ChatMessageDTO.Broadcast> queue = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return ChatMessageDTO.Broadcast.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((ChatMessageDTO.Broadcast) payload);
            }
        });

        // When: 빈 content 전송 (✅ @NotBlank 검증)
        ChatMessageDTO.Request bad = ChatMessageDTO.Request.builder()
                .roomId(1L)
                .content("") // ✅ API 문서: @NotBlank 유효성 검사
                .messageType(MessageType.CHAT)
                .build();
        session.send("/app/chat/send", bad);

        // Then: 브로드캐스트가 오면 안 됨(유효성 실패)
        ChatMessageDTO.Broadcast received = queue.poll(2, TimeUnit.SECONDS);
        assertNull(received, "빈 메시지는 브로드캐스트 되면 안 됩니다.");

        session.disconnect();
        System.out.println("✅ 빈 메시지 전송 유효성 검사 테스트 통과 (API 문서 준수)");
    }

    /**
     * ✅ 테스트용 STOMP 세션 핸들러
     */
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("✅ 채팅 테스트 세션 연결: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("❌ 채팅 테스트 예외: " + exception.getMessage());
            exception.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("❌ 채팅 테스트 전송 오류: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}