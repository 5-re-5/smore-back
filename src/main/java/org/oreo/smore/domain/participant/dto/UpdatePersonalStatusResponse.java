package org.oreo.smore.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePersonalStatusResponse {

    private Long userId;
    private String nickname;
    private Boolean audioEnabled;
    private Boolean videoEnabled;
    private String message;

    public static UpdatePersonalStatusResponse success(Long userId, String nickname,
                                                       Boolean audioEnabled, Boolean videoEnabled) {
        return UpdatePersonalStatusResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .audioEnabled(audioEnabled)
                .videoEnabled(videoEnabled)
                .message("개인 미디어 상태가 성공적으로 변경되었습니다")
                .build();
    }

    @Override
    public String toString() {
        return String.format("UpdatePersonalStatusResponse{userId=%d, nickname='%s', audioEnabled=%s, videoEnabled=%s, message='%s'}",
                userId, nickname, audioEnabled, videoEnabled, message);
    }
}
