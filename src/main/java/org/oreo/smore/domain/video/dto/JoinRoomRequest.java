package org.oreo.smore.domain.video.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class JoinRoomRequest {
    // 스터디룸 입장 요청 DTO

    private String password;

    private Integer tokenExpirySeconds;

    private Boolean canPublish;

    private Boolean canSubscribe;

    public JoinRoomRequest(String password, Integer tokenExpirySeconds,
                           Boolean canPublish, Boolean canSubscribe) {
        this.password = password;
        this.tokenExpirySeconds = tokenExpirySeconds != null ? tokenExpirySeconds : 3600;
        this.canPublish = canPublish != null ? canPublish : true;
        this.canSubscribe = canSubscribe != null ? canSubscribe : true;
    }

    public Integer getTokenExpirySeconds() {
        return tokenExpirySeconds != null ? tokenExpirySeconds : 3600;
    }

    public Boolean getCanPublish() {
        return canPublish != null ? canPublish : true;
    }

    public Boolean getCanSubscribe() {
        return canSubscribe != null ? canSubscribe : true;
    }
}
