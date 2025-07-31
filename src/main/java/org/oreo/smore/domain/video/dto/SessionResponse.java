package org.oreo.smore.domain.video.dto;

import io.openvidu.java.client.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.oreo.smore.domain.studyroom.StudyRoom;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@ToString
public class SessionResponse {

    private String sessionId;
    private Long roomId;
    private String roomTitle;
    private LocalDateTime createdAt;
    private String customSessionId;

    public static SessionResponse of(String sessionId, Long roomId, String roomTitle) {
        return new SessionResponse(sessionId, roomId, roomTitle, LocalDateTime.now(), null);
    }

    public static SessionResponse of(Session session, StudyRoom studyRoom) {
        return new SessionResponse(
                session.getSessionId(),
                studyRoom.getRoomId(),
                studyRoom.getTitle(),
                studyRoom.getCreatedAt(),
                extractCustomSessionId(session.getSessionId())
        );
    }
    // "ses_java_study_room_1" -> "java_study_room_1"
    private static String extractCustomSessionId(String sessionId) {
        return sessionId.startsWith("ses_") ? sessionId.substring("ses_".length()) : sessionId;
    }
}
