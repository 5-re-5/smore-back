package org.oreo.smore.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // 채팅 메시지 저장
    @Transactional
    public ChatMessageDTO.Response saveMessage(ChatMessageDTO.Request request, User user) {
         try {
             log.info("메시지 저장 시작 - 사용자: {}, 룸ID: {}, 내용: {}",
                     user.getNickname(), request.getRoomId(), request.getContent());

             // 사용자 존재 확인
             User foundUser = userRepository.findById(user.getUserId())
                     .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + user.getUserId()));

             // ChatMessage 엔티티 생성
             ChatMessage chatMessage = ChatMessage.builder()
                     .roomId(request.getRoomId())
                     .user(foundUser)
                     .content(request.getContent())
                     .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.CHAT)
                     .build();

             // 메시지 저장
             ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
             log.info("✅ 메시지 저장 완료 - 메시지 ID: {}", savedMessage.getId());

             // 응답 DTO 생성
             return createMessageResponse(savedMessage, foundUser);

         } catch (Exception e) {
             log.error("❌ 메시지 저장 중 오류 발생 - 사용자: {}, 룸ID: {}",
                     user.getUserId(), request.getRoomId(), e);
             throw new RuntimeException("메시지 저장에 실패했습니다", e);
         }
    }

    // 키셋 페이지네이션으로 채팅방 메시지 조회
    public Slice<ChatMessageDTO.Response> getMessagesByRoomId(Long roomId, Long lastMessageId,
                                                              LocalDateTime lastCreatedAt, int size) {
        log.info("📖 채팅방 메시지 조회 - 룸ID: {}, 마지막 메시지 ID: {}, 크기: {}",
                roomId, lastMessageId, size);

        Pageable pageable = PageRequest.of(0, size);
        Slice<ChatMessage> messageSlice = chatMessageRepository.findMessagesByRoomIdWithKeyset(
                roomId, lastMessageId, lastCreatedAt, pageable);

        return messageSlice.map(this::convertToResponseDTO);
    }

    // 특정 시간 이후의 새로운 메시지 조회
    public List<ChatMessageDTO.Response> getRecentMessages(Long roomId, LocalDateTime since) {
        log.info("📱 최근 메시지 조회 - 룸ID: {}, 기준 시간: {}", roomId, since);

        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages(roomId, since);

        return recentMessages.stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    public Optional<ChatMessageDTO.Response> getLatestMessage(Long roomId) {
        log.info("🔍 최신 메시지 조회 - 룸ID: {}", roomId);

        return chatMessageRepository.findLatestMessageByRoomId(roomId)
                .map(this::convertToResponseDTO);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long roomId, Long userId) {
        log.info("🗑️ 메시지 삭제 - 메시지 ID: {}, 룸ID: {}, 사용자 ID: {}", messageId, roomId, userId);

        int deletedCount = chatMessageRepository.softDeleteMessage(messageId, roomId, userId);

        if (deletedCount == 0) {
            throw new IllegalArgumentException("메시지 삭제에 실패했습니다. 권한이 없거나 메시지를 찾을 수 없습니다.");
        }

        log.info("✅ 메시지 삭제 완료 - 메시지 ID: {}", messageId);
    }

    // 채팅방의 총 메시지 개수 조회
    public long getMessageCountByRoom(Long roomId) {
        return chatMessageRepository.countActiveMessagesByRoomId(roomId);
    }

    // 특정 사용자의 최근 활동 메시지 개수
    public long getUserRecentActivityCount(Long userId, LocalDateTime since) {
        return chatMessageRepository.countUserMessagesAfter(userId, since);
    }

    private ChatMessageDTO.Response convertToResponseDTO(ChatMessage message) {
        User user = message.getUser();
        ChatMessageDTO.UserInfo userInfo = null;

        if (user != null) {
            userInfo = ChatMessageDTO.UserInfo.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .profileUrl(user.getProfileUrl())
                    .build();
        }

        return ChatMessageDTO.Response.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .userId(message.getUser() != null ? message.getUser().getUserId() : null)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .user(userInfo)
                .build();
    }

    private ChatMessageDTO.Response createMessageResponse(ChatMessage savedMessage, User user) {
        ChatMessageDTO.UserInfo userInfo = ChatMessageDTO.UserInfo.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileUrl(user.getProfileUrl())
                .build();

        return ChatMessageDTO.Response.builder()
                .messageId(savedMessage.getId())
                .roomId(savedMessage.getRoomId())
                .userId(savedMessage.getUser().getUserId())
                .content(savedMessage.getContent())
                .messageType(savedMessage.getMessageType())
                .createdAt(savedMessage.getCreatedAt())
                .user(userInfo)
                .build();
    }
}
