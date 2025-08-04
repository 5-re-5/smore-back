package org.oreo.smore.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    // 생성된 livekit 액세스 토큰
    private String accessToken;

    // 방 이름
    private String roomName;

    // 사용자 식별자
    private String identity;

    // 토큰 만료 시간
    private LocalDateTime expiresAt;

    // 발행 권한 (마이크 카메라 권한)
    private boolean canPublish;

    // 구독 권한
    private boolean canSubscribe;

    // 토큰 생성 시간
    private LocalDateTime createdAt;

    public TokenResponse(String accessToken, String roomName, String identity,
                         LocalDateTime expiresAt, Boolean canPublish, Boolean canSubscribe,
                         LocalDateTime createdAt) {
        this.accessToken = accessToken;
        this.roomName = roomName;
        this.identity = identity;
        this.expiresAt = expiresAt;
        this.canPublish = canPublish;
        this.canSubscribe = canSubscribe;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt != null ? createdAt : LocalDateTime.now();
    }
}
