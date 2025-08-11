package org.oreo.smore.domain.participant.dto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePersonalStatusRequest {

    @NotNull(message = "오디오 상태는 필수입니다")
    private Boolean audioEnabled;

    @NotNull(message = "비디오 상태는 필수입니다")
    private Boolean videoEnabled;

    // 로깅용 toString
    @Override
    public String toString() {
        return String.format("UpdatePersonalStatusRequest{audioEnabled=%s, videoEnabled=%s}",
                audioEnabled, videoEnabled);
    }

}
