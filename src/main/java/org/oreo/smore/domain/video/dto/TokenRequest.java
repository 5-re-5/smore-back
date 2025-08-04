package org.oreo.smore.domain.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    // 방 이름
    @NotBlank(message = "방 이름은 필수입니다.")
    private String roomName;

    // 사용자 식별자
    @NotBlank(message = "사용자 식별자는 필수입니다.")
    private String identity;

    // 마이크 카메라 사용 가능 여부
    @NotNull(message = "발행 권한 설정은 필수입니다.")
    private Boolean canPublish;

    // 구독 권한
    private Boolean canSubscribe;

    // 토큰 만료 시간
    private Integer tokenExpirySeconds;

    public TokenRequest (String roomName, String identity, Boolean canPublish, Integer tokenExpirySeconds, Boolean canSubscribe) {
        this.roomName = roomName;
        this.identity = identity;
        this.canPublish = canPublish;
        this.canSubscribe = canSubscribe != null ? canSubscribe : true;
        this.tokenExpirySeconds = tokenExpirySeconds != null ? tokenExpirySeconds : 3600;
    }

    public Boolean getCanSubscribe() {
        return canSubscribe != null ? canSubscribe : true;
    }

    public Integer getTokenExpirySeconds() {
        return tokenExpirySeconds != null ? tokenExpirySeconds : 3600;
    }
}
