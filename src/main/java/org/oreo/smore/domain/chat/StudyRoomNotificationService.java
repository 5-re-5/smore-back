package org.oreo.smore.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.chat.dto.ChatMessageDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyRoomNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // 방 삭제 알림 전송
    public void notifyRoomDeleted(Long roomId, String reason) {
        log.warn("🚨 방 삭제 알림 전송 - 방ID: {}, 사유: {}", roomId, reason);

        try {
            // 방 삭제 메시지 생성
            ChatMessageDTO.Broadcast deleteMessage = ChatMessageDTO.Broadcast.builder()
                    .roomId(roomId)
                    .content("방장이 나가서 방이 삭제되었습니다. 메인 페이지로 이동됩니다.")
                    .messageType(MessageType.ROOM_DELETED)
                    .timestamp(LocalDateTime.now())
                    .broadcastType("ROOM_DELETED")
                    .build();

            // 해당 방 참가자들에게만 전송
            String destination = "/topic/study-rooms/" + roomId + "/events";
            messagingTemplate.convertAndSend(destination, deleteMessage);

            log.info("✅ 방 삭제 알림 전송 완료 - 방ID: {}, 목적지: {}", roomId, destination);

        } catch (Exception e) {
            log.error("❌ 방 삭제 알림 전송 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }
}