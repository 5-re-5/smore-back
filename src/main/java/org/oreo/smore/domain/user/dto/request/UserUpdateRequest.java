package org.oreo.smore.domain.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    private String nickname;

    // 파일 확장자와 크기 제한은 Validator 또는 Service에서 별도로 체크
    private MultipartFile profileImage;

    private Boolean removeImage;

    @Size(max = 50, message = "디데이 제목은 최대 50자까지 가능합니다.")
    private String targetDateTitle;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD여야 합니다.")
    private String targetDate;

    @Min(value = 0, message = "목표 공부시간은 0 이상이어야 합니다.")
    @Max(value = 1500, message = "목표 공부시간은 1500 이하이어야 합니다.")
    private Integer goalStudyTime;

    @Size(max = 50, message = "각오는 최대 50자까지 가능합니다.")
    private String determination;
}
