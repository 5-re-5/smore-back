package org.oreo.smore.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class SessionResponse {

    private String sessionId;
    private Long roomId;
    private String roomTitle;

    public static SessionResponse of(String sessionId, Long roomId, String roomTitle) {
        return new SessionResponse(sessionId, roomId, roomTitle);
    }
}
