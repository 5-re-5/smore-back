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

@Controller
@RequiredArgsConstructor
@Slf4j
@Validated
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

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
            MessageType messageType = request.getMessageType() != null ?
                    request.getMessageType() : MessageType.CHAT;

            // 브로드캐스트용 메시지 생성
            ChatMessageDTO.Broadcast broadcastMessage = ChatMessageDTO.Broadcast.builder()
                    .roomId(request.getRoomId())
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .content(request.getContent())
                    .messageType(messageType)
                    .timestamp(LocalDateTime.now())
                    .broadcastType("NEW_MESSAGE")
                    .metadata(createMessageMetadata(user, request))
                    .build();

            // 모든 클라이언트에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/broadcast", broadcastMessage);

            log.info("✅ 메시지 브로드캐스트 완료 - 사용자: {}, 룸ID: {}",
                    user.getNickname(), request.getRoomId());

        } catch (Exception e) {
            log.error("❌ 채팅 메시지 처리 중 오류 발생", e);

            // 에러 메시지를 해당 사용자에게만 전송
            String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
            if (userEmail != null) {
                ChatMessageDTO.Broadcast errorMessage = ChatMessageDTO.Broadcast.builder()
                        .roomId(request.getRoomId())
                        .content("메시지 전송 중 오류가 발생했습니다.")
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
            ChatMessageDTO.Broadcast joinMessage = ChatMessageDTO.Broadcast.builder()
                    .roomId(request.getRoomId())
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .content(user.getNickname() + "님이 입장하셨습니다.")
                    .messageType(MessageType.USER_JOIN)
                    .timestamp(LocalDateTime.now())
                    .broadcastType("USER_JOIN")
                    .build();

            messagingTemplate.convertAndSend("/topic/chat/broadcast", joinMessage);

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

            // 퇴장 알림 메시지
            ChatMessageDTO.Broadcast leaveMessage = ChatMessageDTO.Broadcast.builder()
                    .roomId(request.getRoomId())
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .content(user.getNickname() + "님이 퇴장하셨습니다.")
                    .messageType(MessageType.USER_LEAVE)
                    .timestamp(LocalDateTime.now())
                    .broadcastType("USER_LEAVE")
                    .build();

            messagingTemplate.convertAndSend("/topic/chat/broadcast", leaveMessage);

            log.info("✅ 퇴장 알림 브로드캐스트 완료 - 사용자: {}", user.getNickname());

        } catch (Exception e) {
            log.error("❌ 사용자 퇴장 처리 중 오류 발생", e);
        }
    }

    private Object createMessageMetadata(User user, ChatMessageDTO.Request request) {
        return ChatMessageDTO.UserInfo.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileUrl(user.getProfileUrl())
                .build();
    }
}
