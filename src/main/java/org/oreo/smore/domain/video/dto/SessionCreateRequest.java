package org.oreo.smore.domain.video.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {

    @NotNull(message = "roomId는 필수입니다.")
    private Long roomId;

    @Size(max = 50, message = "커스텀 세션 ID는 최대 50자까지 가능합니다.")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]*$",
            message = "커스텀 세션 ID는 영문자, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다."
    )
    private String customSessionId;

    // roomId만으로 생성하는 생성자
    public SessionCreateRequest(Long roomId) {
        this.roomId = roomId;
        this.customSessionId = null;
    }
}
