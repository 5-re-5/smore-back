package org.oreo.smore.domain.studyroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudyRoomRequest {

    private String title;
    private String description;
    private String password;
    private Integer maxParticipants;
    private String tag;
    private StudyRoomCategory category;
    private Integer focusTime;
    private Integer breakTime;


    // 비밀번호 있는지 확인
    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }

    // 뽀모도르 사용유무 확인
    public boolean hasTimerSettings() {
        return focusTime != null && breakTime != null;
    }
}
