package org.oreo.smore.domain.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {

    @NotNull(message = "roomId는 필수입니다.")
    private Long roomId;

}
