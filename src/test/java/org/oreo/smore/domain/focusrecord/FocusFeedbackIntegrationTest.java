package org.oreo.smore.domain.focusrecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse.FocusTimeDto;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse.FocusTrackDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")   // application-test.yml 사용
class FocusFeedbackIntegrationTest {

    @Autowired
    private FocusFeedbackService feedbackService;

//    @Test
//    @DisplayName("실제 GMS 연동 테스트")
//    void realGmsCall_returnsNonEmptyFeedback() {
//        // ## 1) 테스트용 데이터 준비
//        FocusTimeDto best  = new FocusTimeDto("09:00", "11:00", 90);
//        FocusTimeDto worst = new FocusTimeDto("02:00", "04:00", 10);
//        FocusTrackDto track = new FocusTrackDto(
//                List.of("00","01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22","23"),
//                List.of(10,10,10,10,10,10,10,10,10,90,10,10,10,10,10,10,10,10,10,10,10,10,10,10)
//        );
//
//        // ## 2) 실제 호출
//        String feedback = feedbackService.generateOneLineFeedback(best, worst, 3600, track);
//
//        // ## 3) 결과 검증
//        assertNotNull(feedback, "피드백이 null 이면 안 됩니다");
//        assertFalse(feedback.isBlank(), "빈 문자열이 아니어야 합니다");
//
//        // (선택) 콘솔에 찍어보기
//        System.out.println("▶ 실제 GMS 피드백: " + feedback);
//    }
}
