package org.oreo.smore.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateResponse {

    private Long userId;

    private String name;

    private String email;

    private String nickname;

    private String profileUrl;

    private String createdAt; // YYYY-MM-DD

    private Integer goalStudyTime;

    private String level;

    private String targetDateTitle;

    private String targetDate; // YYYY-MM-DD

    private String determination;
}
