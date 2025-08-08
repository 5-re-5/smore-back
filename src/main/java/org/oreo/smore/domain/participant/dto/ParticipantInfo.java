package org.oreo.smore.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantInfo {

    private Long userId;
    private String nickname;
    private Boolean isOwner;
    private Boolean audioEnabled;
    private Boolean videoEnabled;
    private Integer todayStudyTime;
    private Integer targetStudyTime;
}
