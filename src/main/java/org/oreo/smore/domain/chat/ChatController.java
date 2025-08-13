package org.oreo.smore.domain.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.oreo.smore.domain.user.User;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@Validated
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    // 채팅 메시지 전송
    @MessageMapping("/chat/send")
    public void sendMessage(@Valid @Payload ChatMessageDTO.Request request,
                            SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("💬 채팅 메시지 수신 - 룸ID: {}, 내용: {}", request.getRoomId(), request.getContent());

            // 세션에서 사용자 정보 추출
            User user = (User) headerAccessor.getSessionAttributes().get("user");
            if (user == null) {
                log.error("❌ 사용자 정보를 찾을 수 없습니다 - 세션: {}", headerAccessor.getSessionId());
                return;
            }

            // 메시지 타입 설정 (기본값: CHAT)
            ChatMessageDTO.Response savedMessage = chatService.saveMessage(request, user);
            log.info("메시지 DB 저장 완료 - 메시지 ID: {}", savedMessage.getMessageId());

            // 브로드캐스트용 메시지 생성
            ChatMessageDTO.Broadcast broadcastMessage = ChatMessageDTO.Broadcast.builder()
                    .messageId(savedMessage.getMessageId())
                    .roomId(savedMessage.getRoomId())
                    .userId(savedMessage.getUserId())
                    .nickname(savedMessage.getUser() != null ? savedMessage.getUser().getNickname() : null)
                    .content(savedMessage.getContent())
                    .messageType(savedMessage.getMessageType())
                    .timestamp(savedMessage.getCreatedAt())
                    .broadcastType("NEW_MESSAGE")
                    .metadata(createMessageMetadata(savedMessage))
                    .build();

            // 모든 클라이언트에게 브로드캐스트
            String destination = "/topic/study-rooms/" + savedMessage.getRoomId() + "/chat";
            messagingTemplate.convertAndSend(destination, broadcastMessage);

            log.info("✅ 메시지 브로드캐스트 완료 - 사용자: {}, 룸ID: {}",
                    user.getNickname(), request.getRoomId());

        } catch (Exception e) {
            log.error("❌ 채팅 메시지 처리 중 오류 발생", e);

            // 에러 메시지를 해당 사용자에게만 전송
            String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
            if (userEmail != null) {
                ChatMessageDTO.Broadcast errorMessage = ChatMessageDTO.Broadcast.builder()
                        .roomId(request.getRoomId())
                        .content("메시지 전송 중 오류가 발생했습니다: " + e.getMessage())
                        .messageType(MessageType.SYSTEM)
                        .timestamp(LocalDateTime.now())
                        .broadcastType("ERROR")
                        .build();

                messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat/error", errorMessage);
            }
        }
    }

    // 사용자 입장 알림
    @MessageMapping("/chat/join")
    public void joinRoom(@Payload ChatMessageDTO.Request request,
                         SimpMessageHeaderAccessor headerAccessor) {
        try {
            User user = (User) headerAccessor.getSessionAttributes().get("user");
            if (user == null) {
                log.error("❌ 사용자 정보를 찾을 수 없습니다");
                return;
            }

            log.info("🚪 사용자 입장 - 사용자: {}, 룸ID: {}", user.getNickname(), request.getRoomId());

            // 입장 알림 메시지
            ChatMessageDTO.Request joinRequest = ChatMessageDTO.Request.builder()
                    .roomId(request.getRoomId())
                    .content(user.getNickname() + "님이 입장하셨습니다.")
                    .messageType(MessageType.USER_JOIN)
                    .build();

            ChatMessageDTO.Response savedJoinMessage = chatService.saveMessage(joinRequest, user);

            // 입장 알림 브로드캐스트
            ChatMessageDTO.Broadcast joinMessage = ChatMessageDTO.Broadcast.builder()
                    .messageId(savedJoinMessage.getMessageId())
                    .roomId(savedJoinMessage.getRoomId())
                    .userId(savedJoinMessage.getUserId())
                    .nickname(user.getNickname())
                    .content(savedJoinMessage.getContent())
                    .messageType(MessageType.USER_JOIN)
                    .timestamp(savedJoinMessage.getCreatedAt())
                    .broadcastType("USER_JOIN")
                    .build();

            String destination = "/topic/study-rooms/" + savedJoinMessage.getRoomId() + "/chat";
            messagingTemplate.convertAndSend(destination, joinMessage);

            log.info("✅ 입장 알림 브로드캐스트 완료 - 사용자: {}", user.getNickname());

        } catch (Exception e) {
            log.error("❌ 사용자 입장 처리 중 오류 발생", e);
        }
    }

    // 사용자 퇴장 알림
    @MessageMapping("/chat/leave")
    public void leaveRoom(@Payload ChatMessageDTO.Request request,
                          SimpMessageHeaderAccessor headerAccessor) {
        try {
            User user = (User) headerAccessor.getSessionAttributes().get("user");
            if (user == null) {
                log.error("❌ 사용자 정보를 찾을 수 없습니다");
                return;
            }

            log.info("🚪 사용자 퇴장 - 사용자: {}, 룸ID: {}", user.getNickname(), request.getRoomId());

            // ✅ 시스템 메시지로 퇴장 알림 저장
            ChatMessageDTO.Request leaveRequest = ChatMessageDTO.Request.builder()
                    .roomId(request.getRoomId())
                    .content(user.getNickname() + "님이 퇴장하셨습니다.")
                    .messageType(MessageType.USER_LEAVE)
                    .build();

            ChatMessageDTO.Response savedLeaveMessage = chatService.saveMessage(leaveRequest, user);

            // 퇴장 알림 브로드캐스트
            ChatMessageDTO.Broadcast leaveMessage = ChatMessageDTO.Broadcast.builder()
                    .messageId(savedLeaveMessage.getMessageId())
                    .roomId(savedLeaveMessage.getRoomId())
                    .userId(savedLeaveMessage.getUserId())
                    .nickname(user.getNickname())
                    .content(savedLeaveMessage.getContent())
                    .messageType(MessageType.USER_LEAVE)
                    .timestamp(savedLeaveMessage.getCreatedAt())
                    .broadcastType("USER_LEAVE")
                    .build();

            String destination = "/topic/study-rooms/" + savedLeaveMessage.getRoomId() + "/chat";
            messagingTemplate.convertAndSend(destination, leaveMessage);

            log.info("✅ 퇴장 알림 브로드캐스트 완료 - 사용자: {}", user.getNickname());

        } catch (Exception e) {
            log.error("❌ 사용자 퇴장 처리 중 오류 발생", e);
        }
    }

    private Object createMessageMetadata(ChatMessageDTO.Response savedMessage) {
        return Map.of(
                "messageId", savedMessage.getMessageId(),
                "user", savedMessage.getUser(),
                "savedAt", savedMessage.getCreatedAt()
        );
    }
}
