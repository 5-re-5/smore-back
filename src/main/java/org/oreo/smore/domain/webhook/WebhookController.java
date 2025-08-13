package org.oreo.smore.domain.webhook;

import io.livekit.server.RoomServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final RoomServiceClient roomServiceClient;
    private final WebhookService webhookService;

    @PostMapping("/v1/webhook")
    public ResponseEntity<Void> handle(@RequestBody Map<String, Object> payload) throws Exception {
        String event = (String) payload.get("event");
        Map<String, Object> room = (Map<String, Object>) payload.get("room");
        Map<String, Object> participant = (Map<String, Object>) payload.get("participant");

        String roomName = room != null ? (String) room.get("name") : null;
        String identity = participant != null ? (String) participant.get("identity") : null;

        if ("participant_joined".equals(event)) {
            // 참가자 입장
        } else if ("participant_left".equals(event)) {
            if (webhookService.handleParticipantLeft(roomName, identity) == 1) {
                try {
                    // deleteRoom 호출 → 모든 참가자 강제 분리 + 방 종료
                    roomServiceClient.deleteRoom(roomName).execute();
                } catch (Exception e) {
                    // 로깅 및 재시도/보상 처리
                }
            }
        }

        return ResponseEntity.ok().build();
    }
}
