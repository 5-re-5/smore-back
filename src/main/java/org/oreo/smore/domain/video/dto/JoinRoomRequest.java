package org.oreo.smore.domain.video.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {
    // 스터디룸 입장 요청 DTO

    private String password;

    private Integer tokenExpirySeconds;

    private Boolean canPublish;

    private Boolean canSubscribe;

    private Boolean audioEnabled = true;

    private Boolean videoEnabled = true;

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
