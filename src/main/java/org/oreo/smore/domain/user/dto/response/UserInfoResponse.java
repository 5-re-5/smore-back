package org.oreo.smore.domain.user.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private Long userId;
    private String name;
    private String email;
    private String nickname;
    private String profileUrl;
    private String createdAt;
    private Integer goalStudyTime;
    private String level;
    private String targetDateTitle;
    private String targetDate;
    private String determination;
    private Integer todayStudyMinute;
}
