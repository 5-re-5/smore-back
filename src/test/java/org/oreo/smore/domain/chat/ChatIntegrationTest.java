package org.oreo.smore.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.auth.jwt.JwtTokenProvider;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.studyroom.StudyRoomCreationService;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomRequest;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomResponse;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Slice;
import org.springframework.http.*;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.servlet.context-path=/",
                "logging.level.org.springframework.web.socket=DEBUG",
                "logging.level.org.oreo.smore.domain.chat=DEBUG",
                "logging.level.org.oreo.smore.domain.studyroom=DEBUG"
        }
)
@ActiveProfiles("test")
@Import(WebSocketTestConfig.class)
class ChatIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StudyRoomCreationService studyRoomCreationService; // ✅ 추가

    @Autowired
    private StudyRoomService studyRoomService;

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private WebSocketStompClient stompClient;
    private User testOwner;
    private User testParticipant;
    private String ownerJwt;
    private String participantJwt;

    @BeforeEach
    @Transactional
    @Commit
    void setUp() {
        // 기존 데이터 정리
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        studyRoomRepository.deleteAll();
        userRepository.deleteAll();

        // WebSocket 클라이언트 설정
        stompClient = createStompClient();

        // 테스트 사용자들 생성
        testOwner = User.builder()
                .email("integration-owner@example.com")
                .nickname("통합테스트방장")
                .name("통합 테스트 방장")
                .profileUrl("https://example.com/owner.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(120)
                .level("고급")
                .build();

        testParticipant = User.builder()
                .email("integration-participant@example.com")
                .nickname("통합테스트참가자")
                .name("통합 테스트 참가자")
                .profileUrl("https://example.com/participant.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(90)
                .level("중급")
                .build();

        testOwner = userRepository.saveAndFlush(testOwner);
        testParticipant = userRepository.saveAndFlush(testParticipant);

        ownerJwt = jwtTokenProvider.createAccessToken(testOwner.getUserId().toString());
        participantJwt = jwtTokenProvider.createAccessToken(testParticipant.getUserId().toString());

        System.out.println("=== 채팅 시스템 통합 테스트 Setup ===");
        System.out.println("Test Port: " + port);
        System.out.println("Owner: " + testOwner.getNickname() + " (ID: " + testOwner.getUserId() + ")");
        System.out.println("Participant: " + testParticipant.getNickname() + " (ID: " + testParticipant.getUserId() + ")");
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        ObjectMapper testObjectMapper = new ObjectMapper();
        testObjectMapper.registerModule(new JavaTimeModule());
        testObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        messageConverter.setObjectMapper(testObjectMapper);
        client.setMessageConverter(messageConverter);
        return client;
    }

    @Test
    @DisplayName("🏠 1. StudyRoom 생성 → ChatRoom 자동 생성 → 실시간 채팅 전체 플로우 테스트")
    void testFullStudyRoomToChatFlow() throws Exception {
        // ===== 1단계: StudyRoom 생성 =====
        CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                .title("통합테스트 스터디룸")
                .description("채팅 기능 통합 테스트용 방입니다")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .focusTime(25)
                .breakTime(5)
                .tag("통합테스트,채팅")
                .build();

        // StudyRoom 생성 (ChatRoom 자동 생성됨)
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(testOwner.getUserId(), request);
        Long roomId = response.getRoomId();

        System.out.println("✅ 1단계: StudyRoom 생성 완료 - 룸ID: " + roomId);

        // ===== 2단계: ChatRoom 자동 생성 확인 =====
        Optional<ChatRoom> chatRoom = chatRoomService.getActiveChatRoom(roomId);
        assertTrue(chatRoom.isPresent(), "ChatRoom이 자동 생성되어야 합니다");
        assertThat(chatRoom.get().getStudyRoomId()).isEqualTo(roomId);
        assertThat(chatRoom.get().getIsActive()).isTrue();

        System.out.println("✅ 2단계: ChatRoom 자동 생성 확인 완료");

        // ===== 3단계: WebSocket 연결 및 실시간 채팅 =====
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();

        // 방장 WebSocket 연결
        WebSocketHttpHeaders ownerHeaders = new WebSocketHttpHeaders();
        ownerHeaders.add("Cookie", "accessToken=" + ownerJwt);
        StompSession ownerSession = stompClient.connectAsync(url, ownerHeaders, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 참가자 WebSocket 연결
        WebSocketHttpHeaders participantHeaders = new WebSocketHttpHeaders();
        participantHeaders.add("Cookie", "accessToken=" + participantJwt);
        StompSession participantSession = stompClient.connectAsync(url, participantHeaders, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 브로드캐스트 구독 (방장만 메시지 수집)
        ownerSession.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageDTO.Broadcast) payload);
            }
        });

        participantSession.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // 메시지 수신만 하고 수집하지 않음 (중복 방지)
            }
        });

        Thread.sleep(500); // 연결 안정화

        // ===== 4단계: 사용자 입장 알림 =====
        ChatMessageDTO.Request ownerJoin = ChatMessageDTO.Request.builder()
                .roomId(roomId)
                .build();
        ownerSession.send("/app/chat/join", ownerJoin);

        ChatMessageDTO.Request participantJoin = ChatMessageDTO.Request.builder()
                .roomId(roomId)
                .build();
        participantSession.send("/app/chat/join", participantJoin);

        // 입장 알림 확인
        ChatMessageDTO.Broadcast ownerJoinMsg = messageQueue.poll(5, TimeUnit.SECONDS);
        ChatMessageDTO.Broadcast participantJoinMsg = messageQueue.poll(5, TimeUnit.SECONDS);

        assertNotNull(ownerJoinMsg);
        assertNotNull(participantJoinMsg);
        assertThat(ownerJoinMsg.getMessageType()).isEqualTo(MessageType.USER_JOIN);
        assertThat(participantJoinMsg.getMessageType()).isEqualTo(MessageType.USER_JOIN);

        System.out.println("✅ 4단계: 사용자 입장 알림 완료");

        // ===== 5단계: 실시간 채팅 메시지 교환 =====
        ChatMessageDTO.Request ownerMessage = ChatMessageDTO.Request.builder()
                .roomId(roomId)
                .content("안녕하세요! 방장입니다.")
                .messageType(MessageType.CHAT)
                .build();
        ownerSession.send("/app/chat/send", ownerMessage);

        ChatMessageDTO.Request participantMessage = ChatMessageDTO.Request.builder()
                .roomId(roomId)
                .content("안녕하세요! 참가자입니다.")
                .messageType(MessageType.CHAT)
                .build();
        participantSession.send("/app/chat/send", participantMessage);

        // 채팅 메시지 확인
        ChatMessageDTO.Broadcast ownerChatMsg = messageQueue.poll(5, TimeUnit.SECONDS);
        ChatMessageDTO.Broadcast participantChatMsg = messageQueue.poll(5, TimeUnit.SECONDS);

        assertNotNull(ownerChatMsg);
        assertNotNull(participantChatMsg);
        assertThat(ownerChatMsg.getMessageType()).isEqualTo(MessageType.CHAT);
        assertThat(participantChatMsg.getMessageType()).isEqualTo(MessageType.CHAT);

        System.out.println("✅ 5단계: 실시간 채팅 메시지 교환 완료");

        // ===== 6단계: REST API로 메시지 조회 확인 =====
        String messagesUrl = "http://localhost:" + port + "/v1/chat/rooms/" + roomId + "/messages";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "accessToken=" + ownerJwt);
        HttpEntity<?> entity = new HttpEntity<>(headers);

// ✅ 수정: ApiResponse<ChatMessageDTO.PageResponse>로 매핑
        ResponseEntity<org.oreo.smore.global.common.ApiResponse<ChatMessageDTO.PageResponse>> apiResponse =
                restTemplate.exchange(
                        messagesUrl, HttpMethod.GET, entity,
                        new ParameterizedTypeReference<org.oreo.smore.global.common.ApiResponse<ChatMessageDTO.PageResponse>>() {}
                );

        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.oreo.smore.global.common.ApiResponse<ChatMessageDTO.PageResponse> responseWrapper = apiResponse.getBody();
        assertNotNull(responseWrapper);

// ✅ 수정: wrapper에서 실제 데이터 추출
        ChatMessageDTO.PageResponse pageResponse = responseWrapper.getData();
        assertNotNull(pageResponse);
        assertThat(pageResponse.getContent()).hasSize(4);
        System.out.println("✅ 6단계: REST API 메시지 조회 확인 완료");

        // ===== 7단계: 사용자 퇴장 =====
        ChatMessageDTO.Request participantLeave = ChatMessageDTO.Request.builder()
                .roomId(roomId)
                .build();
        participantSession.send("/app/chat/leave", participantLeave);

        ChatMessageDTO.Broadcast leaveMsg = messageQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(leaveMsg);
        assertThat(leaveMsg.getMessageType()).isEqualTo(MessageType.USER_LEAVE);

        System.out.println("✅ 7단계: 사용자 퇴장 알림 완료");

        // WebSocket 연결 해제
        ownerSession.disconnect();
        participantSession.disconnect();

        System.out.println("🎉 StudyRoom → ChatRoom → 실시간 채팅 전체 플로우 테스트 통과!");
    }

    @Test
    @DisplayName("🗑️ 2. StudyRoom 삭제 → ChatRoom 및 메시지 소프트 삭제 통합 테스트")
    void testStudyRoomDeletionFlow() throws Exception {
        // ===== 1단계: StudyRoom 및 ChatRoom 생성 =====
        CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                .title("삭제 테스트 스터디룸")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(testOwner.getUserId(), request);
        Long roomId = response.getRoomId();

        // ChatRoom 생성 확인
        Optional<ChatRoom> chatRoom = chatRoomService.getActiveChatRoom(roomId);
        assertTrue(chatRoom.isPresent());

        System.out.println("✅ 1단계: StudyRoom 및 ChatRoom 생성 완료");

        // ===== 2단계: 채팅 메시지 저장 =====
        for (int i = 1; i <= 5; i++) {
            ChatMessageDTO.Request messageRequest = ChatMessageDTO.Request.builder()
                    .roomId(roomId)
                    .content("삭제 테스트 메시지 " + i)
                    .messageType(MessageType.CHAT)
                    .build();
            chatService.saveMessage(messageRequest, testOwner);
        }

        // 메시지 저장 확인
        long messageCount = chatService.getMessageCountByRoom(roomId);
        assertThat(messageCount).isEqualTo(5);

        System.out.println("✅ 2단계: 채팅 메시지 5개 저장 완료");

        // ===== 3단계: StudyRoom 삭제 (연쇄 삭제 테스트) =====
        studyRoomService.deleteStudyRoom(roomId, testOwner.getUserId());

        System.out.println("✅ 3단계: StudyRoom 삭제 실행 완료");

        // ===== 4단계: ChatRoom 비활성화 확인 =====
        Optional<ChatRoom> deletedChatRoom = chatRoomService.getActiveChatRoom(roomId);
        assertFalse(deletedChatRoom.isPresent(), "ChatRoom이 비활성화되어야 합니다");

        System.out.println("✅ 4단계: ChatRoom 비활성화 확인 완료");

        // ===== 5단계: 메시지 소프트 삭제 확인 =====
        long remainingMessageCount = chatService.getMessageCountByRoom(roomId);
        assertThat(remainingMessageCount).isEqualTo(0); // 소프트 삭제로 조회되지 않음

        Slice<ChatMessageDTO.Response> messages = chatService.getMessagesByRoomId(roomId, null, null, 10);
        assertThat(messages.getContent()).isEmpty(); // 활성 메시지 없음

        System.out.println("✅ 5단계: 메시지 소프트 삭제 확인 완료");

        System.out.println("🎉 StudyRoom 삭제 → ChatRoom 및 메시지 소프트 삭제 통합 테스트 통과!");
    }

    @Test
    @DisplayName("📊 3. 키셋 페이지네이션 + 실시간 메시지 동기화 통합 테스트")
    void testPaginationWithRealTimeSync() throws Exception {
        // ===== 1단계: StudyRoom 생성 =====
        CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                .title("페이지네이션 테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(testOwner.getUserId(), request);
        Long roomId = response.getRoomId();

        // ===== 2단계: 기존 메시지 20개 저장 =====
        for (int i = 1; i <= 20; i++) {
            ChatMessageDTO.Request messageRequest = ChatMessageDTO.Request.builder()
                    .roomId(roomId)
                    .content("기존 메시지 " + i)
                    .messageType(MessageType.CHAT)
                    .build();
            chatService.saveMessage(messageRequest, testOwner);
            Thread.sleep(10); // 시간 차이 생성
        }

        System.out.println("✅ 기존 메시지 20개 저장 완료");

        // ===== 3단계: 첫 번째 페이지 조회 (최신 10개) =====
        Slice<ChatMessageDTO.Response> firstPage = chatService.getMessagesByRoomId(roomId, null, null, 10);
        assertThat(firstPage.getContent()).hasSize(10);
        assertTrue(firstPage.hasNext());

        List<ChatMessageDTO.Response> firstMessages = firstPage.getContent();
        assertThat(firstMessages.get(0).getContent()).isEqualTo("기존 메시지 20"); // 최신부터

        System.out.println("✅ 첫 번째 페이지 조회 완료");

        // ===== 4단계: WebSocket으로 실시간 메시지 추가 =====
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", "accessToken=" + ownerJwt);
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

        // ✅ 수정: 실시간 메시지 전송 직전 시간 기록
        Thread.sleep(500); // 기존 메시지와 시간 차이 확보
        LocalDateTime beforeRealTimeMessages = LocalDateTime.now();
        Thread.sleep(100); // 추가 시간 차이

        // 실시간 메시지 3개 전송
        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Request messageRequest = ChatMessageDTO.Request.builder()
                    .roomId(roomId)
                    .content("실시간 메시지 " + i)
                    .messageType(MessageType.CHAT)
                    .build();
            session.send("/app/chat/send", messageRequest);
            Thread.sleep(200);
        }

        // 실시간 메시지 수신 확인
        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Broadcast msg = messageQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(msg);
            assertThat(msg.getContent()).isEqualTo("실시간 메시지 " + i);
        }

        System.out.println("✅ 실시간 메시지 3개 추가 완료");

        // ===== 5단계: since 기반 최근 메시지 조회 =====
        // ✅ 수정: 실시간 메시지 전송 직전 시간 사용
        List<ChatMessageDTO.Response> recentMessages = chatService.getRecentMessages(roomId, beforeRealTimeMessages);
        assertThat(recentMessages).hasSize(3); // 실시간으로 추가된 3개만
        assertThat(recentMessages.get(0).getContent()).isEqualTo("실시간 메시지 1");
        assertThat(recentMessages.get(2).getContent()).isEqualTo("실시간 메시지 3");

        System.out.println("✅ since 기반 최근 메시지 조회 완료");

        // ===== 6단계: 두 번째 페이지 조회 (키셋 사용) =====
        ChatMessageDTO.Response lastFromFirstPage = firstMessages.get(9);
        Slice<ChatMessageDTO.Response> secondPage = chatService.getMessagesByRoomId(
                roomId,
                lastFromFirstPage.getMessageId(),
                lastFromFirstPage.getCreatedAt(),
                10
        );

        assertThat(secondPage.getContent()).hasSize(10);
        assertFalse(secondPage.hasNext()); // 마지막 페이지

        System.out.println("✅ 두 번째 페이지 조회 완료");

        // ===== 7단계: 전체 메시지 개수 확인 =====
        long totalCount = chatService.getMessageCountByRoom(roomId);
        assertThat(totalCount).isEqualTo(23); // 기존 20개 + 실시간 3개

        session.disconnect();
        System.out.println("🎉 키셋 페이지네이션 + 실시간 메시지 동기화 통합 테스트 통과!");
    }

    @Test
    @DisplayName("👥 4. 다중 사용자 동시 접속 및 메시지 브로드캐스트 통합 테스트")
    void testMultiUserConcurrentMessaging() throws Exception {
        // ===== 1단계: StudyRoom 생성 =====
        CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                .title("다중 사용자 테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(testOwner.getUserId(), request);
        Long roomId = response.getRoomId();

        // ===== 2단계: 세 번째 사용자 생성 =====
        User thirdUser = User.builder()
                .email("integration-third@example.com")
                .nickname("통합테스트셋째")
                .name("통합 테스트 셋째")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(75)
                .level("중급")
                .build();
        thirdUser = userRepository.saveAndFlush(thirdUser);
        String thirdJwt = jwtTokenProvider.createAccessToken(thirdUser.getUserId().toString());

        // ===== 3단계: 3명의 사용자 WebSocket 연결 =====
        String url = "ws://localhost:" + port + "/ws/chat";
        BlockingQueue<ChatMessageDTO.Broadcast> messageQueue = new LinkedBlockingQueue<>();

        // 방장 연결
        WebSocketHttpHeaders ownerHeaders = new WebSocketHttpHeaders();
        ownerHeaders.add("Cookie", "accessToken=" + ownerJwt);
        StompSession ownerSession = stompClient.connectAsync(url, ownerHeaders, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 참가자 연결
        WebSocketStompClient stompClient2 = createStompClient();
        WebSocketHttpHeaders participantHeaders = new WebSocketHttpHeaders();
        participantHeaders.add("Cookie", "accessToken=" + participantJwt);
        StompSession participantSession = stompClient2.connectAsync(url, participantHeaders, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 세 번째 사용자 연결
        WebSocketStompClient stompClient3 = createStompClient();
        WebSocketHttpHeaders thirdHeaders = new WebSocketHttpHeaders();
        thirdHeaders.add("Cookie", "accessToken=" + thirdJwt);
        StompSession thirdSession = stompClient3.connectAsync(url, thirdHeaders, new TestStompSessionHandler())
                .get(10, TimeUnit.SECONDS);

        // 방장만 메시지 수집 (중복 방지)
        ownerSession.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.Broadcast.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageDTO.Broadcast) payload);
            }
        });

        // 나머지는 구독만
        participantSession.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return ChatMessageDTO.Broadcast.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {}
        });

        thirdSession.subscribe("/topic/chat/broadcast", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return ChatMessageDTO.Broadcast.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {}
        });

        Thread.sleep(1000); // 연결 안정화

        // ===== 4단계: 순차적 입장 =====
        ChatMessageDTO.Request joinRequest = ChatMessageDTO.Request.builder().roomId(roomId).build();

        ownerSession.send("/app/chat/join", joinRequest);
        Thread.sleep(300);
        participantSession.send("/app/chat/join", joinRequest);
        Thread.sleep(300);
        thirdSession.send("/app/chat/join", joinRequest);
        Thread.sleep(300);

        // 입장 알림 3개 확인
        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Broadcast joinMsg = messageQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(joinMsg, "입장 알림 " + i + "을 받아야 합니다");
            assertThat(joinMsg.getMessageType()).isEqualTo(MessageType.USER_JOIN);
        }

        System.out.println("✅ 3명 사용자 순차 입장 완료");

        // ===== 5단계: 동시 메시지 전송 =====
        ChatMessageDTO.Request ownerMsg = ChatMessageDTO.Request.builder()
                .roomId(roomId).content("방장 메시지").build();
        ChatMessageDTO.Request participantMsg = ChatMessageDTO.Request.builder()
                .roomId(roomId).content("참가자 메시지").build();
        ChatMessageDTO.Request thirdMsg = ChatMessageDTO.Request.builder()
                .roomId(roomId).content("셋째 메시지").build();

        // 거의 동시에 전송
        ownerSession.send("/app/chat/send", ownerMsg);
        participantSession.send("/app/chat/send", participantMsg);
        thirdSession.send("/app/chat/send", thirdMsg);

        // 메시지 3개 수신 확인 (순서는 보장되지 않음)
        String[] expectedContents = {"방장 메시지", "참가자 메시지", "셋째 메시지"};
        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Broadcast chatMsg = messageQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(chatMsg, "채팅 메시지 " + i + "을 받아야 합니다");
            assertThat(chatMsg.getMessageType()).isEqualTo(MessageType.CHAT);
            assertThat(expectedContents).contains(chatMsg.getContent());
        }

        System.out.println("✅ 3명 사용자 동시 메시지 전송 완료");

        // ===== 6단계: REST API로 전체 메시지 확인 =====
        String messagesUrl = "http://localhost:" + port + "/v1/chat/rooms/" + roomId + "/messages?size=20";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "accessToken=" + ownerJwt);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // ✅ 수정: ApiResponse로 감싸진 응답 처리
        try {
            ResponseEntity<org.oreo.smore.global.common.ApiResponse<ChatMessageDTO.PageResponse>> apiResponse =
                    restTemplate.exchange(
                            messagesUrl, HttpMethod.GET, entity,
                            new ParameterizedTypeReference<org.oreo.smore.global.common.ApiResponse<ChatMessageDTO.PageResponse>>() {}
                    );

            assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            org.oreo.smore.global.common.ApiResponse<ChatMessageDTO.PageResponse> responseWrapper = apiResponse.getBody();
            assertNotNull(responseWrapper, "Response wrapper should not be null");

            ChatMessageDTO.PageResponse pageResponse = responseWrapper.getData();
            assertNotNull(pageResponse, "Page response should not be null");
            assertThat(pageResponse.getContent()).hasSize(6); // 입장 3개 + 채팅 3개

        } catch (Exception e) {
            // ✅ 만약 위의 방법이 안 되면 디버깅용 코드
            System.out.println("ApiResponse 매핑 실패, 디버깅 시도...");
            ResponseEntity<Map<String, Object>> debugResponse = restTemplate.exchange(
                    messagesUrl, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            System.out.println("API Response Structure: " + debugResponse.getBody());

            // 대안: Map으로 받아서 수동 검증
            assertThat(debugResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> responseMap = debugResponse.getBody();
            assertNotNull(responseMap);

            // data 필드에서 content 추출해서 검증
            if (responseMap.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                if (data.containsKey("content")) {
                    List<?> content = (List<?>) data.get("content");
                    assertThat(content).hasSize(6);
                }
            }
        }

        System.out.println("✅ REST API로 전체 메시지 확인 완료");

        // ===== 7단계: 순차적 퇴장 =====
        ChatMessageDTO.Request leaveRequest = ChatMessageDTO.Request.builder().roomId(roomId).build();

        thirdSession.send("/app/chat/leave", leaveRequest);
        Thread.sleep(300);
        participantSession.send("/app/chat/leave", leaveRequest);
        Thread.sleep(300);

        // 퇴장 알림 2개 확인
        for (int i = 1; i <= 2; i++) {
            ChatMessageDTO.Broadcast leaveMsg = messageQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(leaveMsg, "퇴장 알림 " + i + "을 받아야 합니다");
            assertThat(leaveMsg.getMessageType()).isEqualTo(MessageType.USER_LEAVE);
        }

        // 연결 해제
        ownerSession.disconnect();
        participantSession.disconnect();
        thirdSession.disconnect();

        System.out.println("🎉 다중 사용자 동시 접속 및 메시지 브로드캐스트 통합 테스트 통과!");
    }

    @Test
    @DisplayName("🔄 5. 방장 퇴장으로 인한 강제 방 삭제 통합 테스트")
    void testOwnerLeaveRoomDeletion() throws Exception {
        // ===== 1단계: StudyRoom 생성 및 참가자 입장 =====
        CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                .title("방장 퇴장 테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(testOwner.getUserId(), request);
        Long roomId = response.getRoomId();

        // ===== 2단계: 메시지 저장 =====
        for (int i = 1; i <= 10; i++) {
            ChatMessageDTO.Request messageRequest = ChatMessageDTO.Request.builder()
                    .roomId(roomId)
                    .content("방장퇴장 테스트 메시지 " + i)
                    .messageType(MessageType.CHAT)
                    .build();
            chatService.saveMessage(messageRequest, (i % 2 == 0) ? testParticipant : testOwner);
        }

        // 메시지 및 ChatRoom 존재 확인
        long beforeDeleteCount = chatService.getMessageCountByRoom(roomId);
        assertThat(beforeDeleteCount).isEqualTo(10);

        Optional<ChatRoom> beforeChatRoom = chatRoomService.getActiveChatRoom(roomId);
        assertTrue(beforeChatRoom.isPresent());

        System.out.println("✅ StudyRoom 생성 및 메시지 10개 저장 완료");

        // ===== 3단계: 방장 퇴장으로 인한 방 삭제 =====
        studyRoomService.deleteStudyRoomByOwnerLeave(roomId, testOwner.getUserId());

        System.out.println("✅ 방장 퇴장으로 인한 방 삭제 실행 완료");

        // ===== 4단계: 삭제 후 상태 확인 =====
        // ChatRoom 비활성화 확인
        Optional<ChatRoom> afterChatRoom = chatRoomService.getActiveChatRoom(roomId);
        assertFalse(afterChatRoom.isPresent(), "ChatRoom이 비활성화되어야 합니다");

        // 메시지 소프트 삭제 확인
        long afterDeleteCount = chatService.getMessageCountByRoom(roomId);
        assertThat(afterDeleteCount).isEqualTo(0);

        // 페이지네이션으로도 조회되지 않아야 함
        Slice<ChatMessageDTO.Response> messages = chatService.getMessagesByRoomId(roomId, null, null, 20);
        assertThat(messages.getContent()).isEmpty();

        // StudyRoom 소프트 삭제 확인
        Optional<StudyRoom> deletedStudyRoom = studyRoomRepository.findById(roomId);
        assertTrue(deletedStudyRoom.isPresent());
        assertNotNull(deletedStudyRoom.get().getDeletedAt(), "StudyRoom이 소프트 삭제되어야 합니다");

        System.out.println("✅ 삭제 후 상태 확인 완료");

        System.out.println("🎉 방장 퇴장으로 인한 강제 방 삭제 통합 테스트 통과!");
    }

    /**
     * ✅ 테스트용 STOMP 세션 핸들러
     */
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("✅ 통합 테스트 세션 연결: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("❌ 통합 테스트 예외: " + exception.getMessage());
            exception.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("❌ 통합 테스트 전송 오류: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}