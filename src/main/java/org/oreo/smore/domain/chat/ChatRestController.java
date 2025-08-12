package org.oreo.smore.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRestController {

    private final ChatService chatService;

    // 키셋 페이지네이션으로 채팅방 메시지 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ChatMessageDTO.PageResponse getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime lastCreatedAt) {

        log.info("📖 채팅방 메시지 조회 요청 - 룸ID: {}, 크기: {}, 마지막 메시지 ID: {}",
                roomId, size, lastMessageId);

        Slice<ChatMessageDTO.Response> messageSlice = chatService.getMessagesByRoomId(
                roomId, lastMessageId, lastCreatedAt, size);

        // ✅ API 문서에 맞는 응답 형태로 변환
        ChatMessageDTO.PageResponse pageResponse = ChatMessageDTO.PageResponse.builder()
                .content(messageSlice.getContent())
                .hasNext(messageSlice.hasNext())
                .size(size)
                .build();

        // 다음 페이지가 있는 경우 커서 정보 추가
        if (messageSlice.hasNext() && !messageSlice.getContent().isEmpty()) {
            List<ChatMessageDTO.Response> content = messageSlice.getContent();
            ChatMessageDTO.Response lastMessage = content.get(content.size() - 1);

            ChatMessageDTO.PageCursor nextCursor = ChatMessageDTO.PageCursor.builder()
                    .lastMessageId(lastMessage.getMessageId())
                    .lastCreatedAt(lastMessage.getCreatedAt())
                    .build();

            pageResponse.setNextCursor(nextCursor);
        }

        return pageResponse;
    }

    // 특정 시간 이후의 최근 메시지 조회
    @GetMapping("/rooms/{roomId}/messages/recent")
    public List<ChatMessageDTO.Response> getRecentMessages(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime since) {

        log.info("📱 최근 메시지 조회 요청 - 룸ID: {}, 기준 시간: {}", roomId, since);

        return chatService.getRecentMessages(roomId, since);
    }

    // 채팅방의 최신 메시지 1개 조회
    @GetMapping("/rooms/{roomId}/messages/latest")
    public ChatMessageDTO.Response getLatestMessage(@PathVariable Long roomId) {

        log.info("🔍 최신 메시지 조회 요청 - 룸ID: {}", roomId);

        return chatService.getLatestMessage(roomId).orElse(null);
    }

    // 메시지 삭제
    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public String deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestParam Long userId) { // 실제로는 인증에서 가져와야 함

        log.info("🗑️ 메시지 삭제 요청 - 룸ID: {}, 메시지 ID: {}, 사용자 ID: {}", roomId, messageId, userId);

        chatService.deleteMessage(messageId, roomId, userId);
        return "메시지가 삭제되었습니다.";
    }

    // 채팅방 메시지 개수 조회
    @GetMapping("/rooms/{roomId}/messages/count")
    public Long getMessageCount(@PathVariable Long roomId) {

        log.info("📊 메시지 개수 조회 요청 - 룸ID: {}", roomId);

        return chatService.getMessageCountByRoom(roomId);
    }
}