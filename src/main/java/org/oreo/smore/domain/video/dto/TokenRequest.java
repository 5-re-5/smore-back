package org.oreo.smore.domain.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class TokenRequest {

    @NotBlank(message = "방 이름은 필수입니다.")
    private String roomName;

    @NotBlank(message = "사용자 식별자는 필수입니다.")
    private String identity;   // 보통 userId 사용 + 프로필 url 매칭

    @NotNull(message = "발행 권한 설정은 필수입니다.")
    private Boolean canPublish;

    private Boolean canSubscribe;

    private Integer tokenExpirySeconds;

    public TokenRequest(String roomName, String identity, Boolean canPublish,
                        Boolean canSubscribe, Integer tokenExpirySeconds) {
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