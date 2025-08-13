package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.chat.WebSocketTestConfig;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatService 키셋 페이지네이션 테스트
 * 무한 스크롤 기능 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WebSocketTestConfig.class)
@Transactional
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User secondUser;
    private Long testRoomId = 1L;

    @BeforeEach
    @Commit
    void setUp() {
        // 기존 데이터 정리
        chatMessageRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 사용자들 생성
        testUser = User.builder()
                .email("chatservice-test@example.com")
                .nickname("서비스테스터")
                .name("서비스 테스트")
                .profileUrl("https://example.com/profile1.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(60)
                .level("초급")
                .build();

        secondUser = User.builder()
                .email("chatservice-test2@example.com")
                .nickname("서비스테스터2")
                .name("서비스 테스트2")
                .profileUrl("https://example.com/profile2.jpg")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(90)
                .level("중급")
                .build();

        testUser = userRepository.saveAndFlush(testUser);
        secondUser = userRepository.saveAndFlush(secondUser);

        System.out.println("=== ChatService 키셋 페이지네이션 테스트 Setup ===");
        System.out.println("Test User 1: " + testUser.getNickname() + " (ID: " + testUser.getUserId() + ")");
        System.out.println("Test User 2: " + secondUser.getNickname() + " (ID: " + secondUser.getUserId() + ")");
    }

    @Test
    @DisplayName("1. 기본 메시지 저장 테스트")
    void testSaveMessage() {
        // Given
        ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                .roomId(testRoomId)
                .content("첫 번째 테스트 메시지입니다.")
                .messageType(MessageType.CHAT)
                .build();

        // When
        ChatMessageDTO.Response savedMessage = chatService.saveMessage(request, testUser);

        // Then
        assertNotNull(savedMessage);
        assertThat(savedMessage.getMessageId()).isNotNull();
        assertThat(savedMessage.getRoomId()).isEqualTo(testRoomId);
        assertThat(savedMessage.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(savedMessage.getContent()).isEqualTo("첫 번째 테스트 메시지입니다.");
        assertThat(savedMessage.getMessageType()).isEqualTo(MessageType.CHAT);
        assertNotNull(savedMessage.getCreatedAt());

        // 사용자 정보 확인
        assertNotNull(savedMessage.getUser());
        assertThat(savedMessage.getUser().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(savedMessage.getUser().getNickname()).isEqualTo(testUser.getNickname());

        System.out.println("✅ 기본 메시지 저장 테스트 통과");
    }

    @Test
    @DisplayName("2. 키셋 페이지네이션 무한 스크롤 테스트")
    void testKeysetPaginationForInfiniteScroll() {
        // Given - 10개 메시지 저장 (시간 간격을 두고)
        ChatMessageDTO.Response[] savedMessages = new ChatMessageDTO.Response[10];
        for (int i = 1; i <= 10; i++) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content("무한스크롤 테스트 메시지 " + i)
                    .messageType(MessageType.CHAT)
                    .build();
            savedMessages[i-1] = chatService.saveMessage(request, testUser);

            // 메시지 간 시간 차이 생성
            try { Thread.sleep(50); } catch (InterruptedException e) {}
        }

        // When - 첫 번째 페이지 조회 (최신 5개)
        Slice<ChatMessageDTO.Response> firstPage = chatService.getMessagesByRoomId(
                testRoomId, null, null, 5);

        // Then - 첫 번째 페이지 검증
        assertThat(firstPage.getContent()).hasSize(5);
        assertTrue(firstPage.hasNext()); // 다음 페이지 존재

        List<ChatMessageDTO.Response> firstMessages = firstPage.getContent();
        // 최신 메시지부터 (DESC 정렬)
        assertThat(firstMessages.get(0).getContent()).isEqualTo("무한스크롤 테스트 메시지 10");
        assertThat(firstMessages.get(4).getContent()).isEqualTo("무한스크롤 테스트 메시지 6");

        // When - 두 번째 페이지 조회 (키셋 사용)
        ChatMessageDTO.Response lastMessageFromFirstPage = firstMessages.get(4);
        Slice<ChatMessageDTO.Response> secondPage = chatService.getMessagesByRoomId(
                testRoomId,
                lastMessageFromFirstPage.getMessageId(),
                lastMessageFromFirstPage.getCreatedAt(),
                5);

        // Then - 두 번째 페이지 검증
        assertThat(secondPage.getContent()).hasSize(5);
        assertFalse(secondPage.hasNext()); // 마지막 페이지

        List<ChatMessageDTO.Response> secondMessages = secondPage.getContent();
        assertThat(secondMessages.get(0).getContent()).isEqualTo("무한스크롤 테스트 메시지 5");
        assertThat(secondMessages.get(4).getContent()).isEqualTo("무한스크롤 테스트 메시지 1");

        // 중복 없이 모든 메시지가 조회되었는지 확인
        assertThat(firstMessages.get(4).getMessageId()).isNotEqualTo(secondMessages.get(0).getMessageId());

        System.out.println("✅ 키셋 페이지네이션 무한 스크롤 테스트 통과");
    }

    @Test
    @DisplayName("3. 실시간 동기화용 최근 메시지 조회 테스트")
    void testGetRecentMessagesForRealTimeSync() {
        // Given - 기준 시간 이전 메시지들 먼저 저장
        for (int i = 1; i <= 3; i++) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content("기존 메시지 " + i)
                    .build();
            chatService.saveMessage(request, testUser);
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }

        // ✅ 기준 시간을 기존 메시지 저장 후로 설정
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        LocalDateTime baseTime = LocalDateTime.now();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // 기준 시간 이후 새로운 메시지들 저장
        for (int i = 1; i <= 5; i++) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content("새로운 메시지 " + i)
                    .build();
            chatService.saveMessage(request, testUser);
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }

        // When - 기준 시간 이후 메시지만 조회
        List<ChatMessageDTO.Response> recentMessages = chatService.getRecentMessages(testRoomId, baseTime);

        // Then
        assertThat(recentMessages).hasSize(5);

        // ASC 정렬이므로 오래된 것부터
        assertThat(recentMessages.get(0).getContent()).isEqualTo("새로운 메시지 1");
        assertThat(recentMessages.get(4).getContent()).isEqualTo("새로운 메시지 5");

        System.out.println("✅ 실시간 동기화용 최근 메시지 조회 테스트 통과");
    }

    @Test
    @DisplayName("4. 최신 메시지 1개 조회 테스트")
    void testGetLatestMessage() {
        // Given - 여러 메시지 저장
        for (int i = 1; i <= 5; i++) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content("순서 테스트 메시지 " + i)
                    .build();
            chatService.saveMessage(request, testUser);
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }

        // When
        Optional<ChatMessageDTO.Response> latestMessage = chatService.getLatestMessage(testRoomId);

        // Then
        assertTrue(latestMessage.isPresent());
        assertThat(latestMessage.get().getContent()).isEqualTo("순서 테스트 메시지 5");

        System.out.println("✅ 최신 메시지 조회 테스트 통과");
    }

    @Test
    @DisplayName("5. 메시지 소프트 삭제 테스트")
    void testSoftDeleteMessage() {
        // Given
        ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                .roomId(testRoomId)
                .content("삭제될 메시지")
                .messageType(MessageType.CHAT)
                .build();

        ChatMessageDTO.Response savedMessage = chatService.saveMessage(request, testUser);

        // When
        chatService.deleteMessage(
                savedMessage.getMessageId(),
                savedMessage.getRoomId(),
                testUser.getUserId()
        );

        // Then - 키셋 페이지네이션 조회에서 삭제된 메시지는 나오지 않아야 함
        Slice<ChatMessageDTO.Response> messages = chatService.getMessagesByRoomId(testRoomId, null, null, 10);
        assertThat(messages.getContent()).isEmpty();

        System.out.println("✅ 메시지 소프트 삭제 테스트 통과");
    }

    @Test
    @DisplayName("6. 채팅방 메시지 통계 테스트")
    void testMessageStatistics() {
        // Given - 여러 사용자가 메시지 작성
        for (int i = 1; i <= 7; i++) {
            User sender = (i % 2 == 0) ? secondUser : testUser;
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content(sender.getNickname() + "의 메시지 " + i)
                    .build();
            chatService.saveMessage(request, sender);
        }

        // When
        long totalCount = chatService.getMessageCountByRoom(testRoomId);
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        long testUserActivity = chatService.getUserRecentActivityCount(testUser.getUserId(), since);
        long secondUserActivity = chatService.getUserRecentActivityCount(secondUser.getUserId(), since);

        // Then
        assertThat(totalCount).isEqualTo(7);
        assertThat(testUserActivity).isEqualTo(4); // 1, 3, 5, 7번 메시지
        assertThat(secondUserActivity).isEqualTo(3); // 2, 4, 6번 메시지

        System.out.println("✅ 채팅방 메시지 통계 테스트 통과");
    }

    @Test
    @DisplayName("7. 시스템 메시지 저장 테스트")
    void testSystemMessageSave() {
        // Given - 시스템 메시지 요청
        ChatMessageDTO.Request systemRequest = ChatMessageDTO.Request.builder()
                .roomId(testRoomId)
                .content("스터디룸이 시작되었습니다.")
                .messageType(MessageType.SYSTEM)
                .build();

        // When
        ChatMessageDTO.Response systemMessage = chatService.saveMessage(systemRequest, testUser);

        // Then
        assertThat(systemMessage.getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(systemMessage.getContent()).isEqualTo("스터디룸이 시작되었습니다.");
        assertNotNull(systemMessage.getUser()); // 시스템 메시지도 작성자 정보는 있음

        System.out.println("✅ 시스템 메시지 저장 테스트 통과");
    }

    @Test
    @DisplayName("8. 대용량 메시지 키셋 페이지네이션 성능 테스트")
    void testLargeDataKeysetPagination() {
        // Given - 대량 메시지 저장 (100개)
        for (int i = 1; i <= 100; i++) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content("대용량 테스트 메시지 " + i)
                    .build();
            chatService.saveMessage(request, testUser);
        }

        // When - 여러 페이지에 걸쳐 조회
        int totalRetrieved = 0;
        int pageSize = 20;
        Long lastMessageId = null;
        LocalDateTime lastCreatedAt = null;

        while (true) {
            Slice<ChatMessageDTO.Response> page = chatService.getMessagesByRoomId(
                    testRoomId, lastMessageId, lastCreatedAt, pageSize);

            totalRetrieved += page.getContent().size();

            if (!page.hasNext()) {
                break;
            }

            // 다음 페이지를 위한 커서 설정
            ChatMessageDTO.Response lastMessage = page.getContent().get(page.getContent().size() - 1);
            lastMessageId = lastMessage.getMessageId();
            lastCreatedAt = lastMessage.getCreatedAt();
        }

        // Then
        assertThat(totalRetrieved).isEqualTo(100);

        System.out.println("✅ 대용량 메시지 키셋 페이지네이션 성능 테스트 통과");
    }

    @Test
    @DisplayName("9. 다양한 메시지 타입 저장 및 조회 테스트")
    void testVariousMessageTypes() {
        // Given & When & Then
        MessageType[] messageTypes = {
                MessageType.CHAT,
                MessageType.SYSTEM,
                MessageType.USER_JOIN,
                MessageType.USER_LEAVE,
                MessageType.FOCUS_START,
                MessageType.BREAK_START
        };

        for (MessageType type : messageTypes) {
            ChatMessageDTO.Request request = ChatMessageDTO.Request.builder()
                    .roomId(testRoomId)
                    .content(type.name() + " 타입 메시지 테스트")
                    .messageType(type)
                    .build();

            ChatMessageDTO.Response savedMessage = chatService.saveMessage(request, testUser);
            assertThat(savedMessage.getMessageType()).isEqualTo(type);
        }

        // 저장된 메시지 개수 확인
        Slice<ChatMessageDTO.Response> allMessages = chatService.getMessagesByRoomId(testRoomId, null, null, 10);
        assertThat(allMessages.getContent()).hasSize(messageTypes.length);

        System.out.println("✅ 다양한 메시지 타입 테스트 통과");
    }
}