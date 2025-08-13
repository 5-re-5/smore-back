//package org.oreo.smore.domain.studyroom.dto;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.oreo.smore.domain.studyroom.StudyRoomCategory;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DisplayName("스터디룸 생성 요청 DTO 테스트")
//class CreateStudyRoomRequestTest {
//
//    @Test
//    void Builder_패턴으로_완전한_DTO_생성() {
//        // given & when
//        CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
//                .title("Java 스터디")
//                .description("매주 화요일 Java 기초 스터디")
//                .password("1234")
//                .maxParticipants(6)
//                .tag("개발")
//                .category(StudyRoomCategory.EMPLOYMENT)
//                .focusTime(25)
//                .breakTime(5)
//                .build();
//
//        // then
//        assertThat(request.getTitle()).isEqualTo("Java 스터디");
//        assertThat(request.getDescription()).isEqualTo("매주 화요일 Java 기초 스터디");
//        assertThat(request.getPassword()).isEqualTo("1234");
//        assertThat(request.getMaxParticipants()).isEqualTo(6);
//        assertThat(request.getTag()).isEqualTo("개발");
//        assertThat(request.getCategory()).isEqualTo(StudyRoomCategory.EMPLOYMENT);
//        assertThat(request.getFocusTime()).isEqualTo(25);
//        assertThat(request.getBreakTime()).isEqualTo(5);
//        assertThat(request.hasPassword()).isTrue();
//        assertThat(request.hasTimerSettings()).isTrue();
//    }
//
//    @Test
//    void NoArgsConstructor로_기본_생성자_동작_확인() {
//        // given & when
//        CreateStudyRoomRequest request = new CreateStudyRoomRequest();
//
//        // then
//        assertThat(request).isNotNull();
//        assertThat(request.getTitle()).isNull();
//        assertThat(request.getDescription()).isNull();
//        assertThat(request.getPassword()).isNull();
//        assertThat(request.getMaxParticipants()).isNull();
//        assertThat(request.getTag()).isNull();
//        assertThat(request.getCategory()).isNull();
//        assertThat(request.getFocusTime()).isNull();
//        assertThat(request.getBreakTime()).isNull();
//        assertThat(request.hasPassword()).isFalse();
//        assertThat(request.hasTimerSettings()).isFalse();
//    }
//
//    @Test
//    void AllArgsConstructor로_DTO_생성() {
//        // given & when
//        CreateStudyRoomRequest request = new CreateStudyRoomRequest(
//                "생성자 테스트 스터디",
//                "AllArgsConstructor로 생성",
//                "password123",
//                6,
//                "테스트태그",
//                StudyRoomCategory.CERTIFICATION,
//                40,
//                10
//        );
//
//        // then
//        assertThat(request.getTitle()).isEqualTo("생성자 테스트 스터디");
//        assertThat(request.getDescription()).isEqualTo("AllArgsConstructor로 생성");
//        assertThat(request.getPassword()).isEqualTo("password123");
//        assertThat(request.getMaxParticipants()).isEqualTo(6);
//        assertThat(request.getTag()).isEqualTo("테스트태그");
//        assertThat(request.getCategory()).isEqualTo(StudyRoomCategory.CERTIFICATION);
//        assertThat(request.getFocusTime()).isEqualTo(40);
//        assertThat(request.getBreakTime()).isEqualTo(10);
//        assertThat(request.hasPassword()).isTrue();
//        assertThat(request.hasTimerSettings()).isTrue();
//    }
//
//    @Test
//    @DisplayName("비밀번호 유무 판단 - hasPassword 메서드")
//    void 비밀번호_유무_판단_hasPassword_메서드() {
//        // given
//        CreateStudyRoomRequest requestWithPassword = CreateStudyRoomRequest.builder()
//                .password("1234")
//                .build();
//
//        CreateStudyRoomRequest requestWithoutPassword = CreateStudyRoomRequest.builder()
//                .password(null)
//                .build();
//
//        CreateStudyRoomRequest requestWithBlankPassword = CreateStudyRoomRequest.builder()
//                .password("   ") // 공백만 있는 비밀번호
//                .build();
//
//        CreateStudyRoomRequest requestWithEmptyPassword = CreateStudyRoomRequest.builder()
//                .password("") // 빈 문자열
//                .build();
//
//        // then
//        assertThat(requestWithPassword.hasPassword()).isTrue();
//        assertThat(requestWithoutPassword.hasPassword()).isFalse();
//        assertThat(requestWithBlankPassword.hasPassword()).isFalse();
//        assertThat(requestWithEmptyPassword.hasPassword()).isFalse();
//    }
//
//    @Test
//    @DisplayName("타이머 설정 유무 판단 - hasTimerSettings 메서드")
//    void 타이머_설정_유무_판단_hasTimerSettings_메서드() {
//        // given
//        CreateStudyRoomRequest requestWithTimer = CreateStudyRoomRequest.builder()
//                .focusTime(25)
//                .breakTime(5)
//                .build();
//
//        CreateStudyRoomRequest requestWithoutTimer = CreateStudyRoomRequest.builder()
//                .focusTime(null)
//                .breakTime(null)
//                .build();
//
//        CreateStudyRoomRequest requestWithPartialTimer1 = CreateStudyRoomRequest.builder()
//                .focusTime(25)
//                .breakTime(null) // 하나만 있음
//                .build();
//
//        CreateStudyRoomRequest requestWithPartialTimer2 = CreateStudyRoomRequest.builder()
//                .focusTime(null)
//                .breakTime(5) // 하나만 있음
//                .build();
//
//        // then
//        assertThat(requestWithTimer.hasTimerSettings()).isTrue();
//        assertThat(requestWithoutTimer.hasTimerSettings()).isFalse();
//        assertThat(requestWithPartialTimer1.hasTimerSettings()).isFalse();
//        assertThat(requestWithPartialTimer2.hasTimerSettings()).isFalse();
//    }
//
//    @Test
//    @DisplayName("모든 카테고리로 DTO 생성 가능")
//    void 모든_카테고리로_DTO_생성_가능() {
//        // given
//        StudyRoomCategory[] allCategories = StudyRoomCategory.values();
//
//        for (StudyRoomCategory category : allCategories) {
//            // when
//            CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
//                    .title(category.name() + " 스터디")
//                    .maxParticipants(6)
//                    .category(category)
//                    .build();
//
//            // then
//            assertThat(request.getCategory()).isEqualTo(category);
//            assertThat(request.getTitle()).isEqualTo(category.name() + " 스터디");
//        }
//    }
//
//    @Test
//    @DisplayName("null 값들도 정상적으로 전송되는지 확인")
//    void null_값들도_정상적으로_전송되는지_확인() {
//        // given & when
//        CreateStudyRoomRequest nullRequest = CreateStudyRoomRequest.builder()
//                .title(null)
//                .description(null)
//                .password(null)
//                .maxParticipants(null)
//                .tag(null)
//                .category(null)
//                .focusTime(null)
//                .breakTime(null)
//                .build();
//
//        // then - 데이터 전송 기능 확인
//        assertThat(nullRequest.getTitle()).isNull();
//        assertThat(nullRequest.getDescription()).isNull();
//        assertThat(nullRequest.getPassword()).isNull();
//        assertThat(nullRequest.getMaxParticipants()).isNull();
//        assertThat(nullRequest.getTag()).isNull();
//        assertThat(nullRequest.getCategory()).isNull();
//        assertThat(nullRequest.getFocusTime()).isNull();
//        assertThat(nullRequest.getBreakTime()).isNull();
//        assertThat(nullRequest.hasPassword()).isFalse();
//        assertThat(nullRequest.hasTimerSettings()).isFalse();
//    }
//}