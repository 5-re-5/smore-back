package org.oreo.smore.domain.studyroom.dto;

import lombok.*;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.springframework.web.multipart.MultipartFile;

@Data
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
    private MultipartFile roomImage;


    // 비밀번호 있는지 확인
    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }

    // 뽀모도르 사용유무 확인
    public boolean hasTimerSettings() {
        return focusTime != null && breakTime != null;
    }
}
