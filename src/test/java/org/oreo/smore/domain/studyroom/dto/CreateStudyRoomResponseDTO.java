package org.oreo.smore.domain.studyroom.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("스터디룸 생성 DTO 테스트")
class CreateStudyRoomResponseTest {

    private StudyRoom testStudyRoom;
    private LocalDateTime testCreatedAt;

    @BeforeEach
    void setUp() {
        testCreatedAt = LocalDateTime.of(2025, 1, 15, 14, 30, 0);

        testStudyRoom = StudyRoom.builder()
                .userId(123L)
                .title("Java 스터디")
                .description("매주 화요일 Java 기초 스터디")
                .password("1234")
                .maxParticipants(6)
                .tag("개발")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(25)
                .breakTime(5)
                .inviteHashCode("ABC123DEF456")
                .build();

        try {
            var roomIdField = StudyRoom.class.getDeclaredField("roomId");
            roomIdField.setAccessible(true);
            roomIdField.set(testStudyRoom, 100L);

            var createdAtField = StudyRoom.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(testStudyRoom, testCreatedAt);
        } catch (Exception e) {
            throw new RuntimeException("테스트 설정 실패", e);
        }
    }

    @Test
    @DisplayName("StudyRoom 엔티티로부터 응답 생성 성공")
    void StudyRoom_엔티티로부터_응답_생성_성공() {
        // when
        CreateStudyRoomResponse response = CreateStudyRoomResponse.from(testStudyRoom);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("Java 스터디");
        assertThat(response.getDescription()).isEqualTo("매주 화요일 Java 기초 스터디");
        assertThat(response.isHasPassword()).isTrue();
        assertThat(response.getMaxParticipants()).isEqualTo(6);
        assertThat(response.getTag()).isEqualTo("개발");
        assertThat(response.getCategory()).isEqualTo(StudyRoomCategory.EMPLOYMENT);
        assertThat(response.getFocusTime()).isEqualTo(25);
        assertThat(response.getBreakTime()).isEqualTo(5);
        assertThat(response.getInviteHashCode()).isEqualTo("ABC123DEF456");
        assertThat(response.getCreatedAt()).isEqualTo(testCreatedAt);
        assertThat(response.getOwnerId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("비밀번호가 없는 스터디룸의 응답 생성")
    void 비밀번호가_없는_스터디룸의_응답_생성() {
        // given
        StudyRoom publicStudyRoom = StudyRoom.builder()
                .userId(456L)
                .title("공개 스터디룸")
                .password(null) // 비밀번호 없음
                .maxParticipants(6)
                .category(StudyRoomCategory.LANGUAGE)
                .build();

        // roomId 설정
        try {
            var roomIdField = StudyRoom.class.getDeclaredField("roomId");
            roomIdField.setAccessible(true);
            roomIdField.set(publicStudyRoom, 200L);
        } catch (Exception e) {
            throw new RuntimeException("테스트 설정 실패", e);
        }

        // when
        CreateStudyRoomResponse response = CreateStudyRoomResponse.from(publicStudyRoom);

        // then
        assertThat(response.isHasPassword()).isFalse();
        assertThat(response.getRoomId()).isEqualTo(200L);
        assertThat(response.getTitle()).isEqualTo("공개 스터디룸");
        assertThat(response.getOwnerId()).isEqualTo(456L);
    }

    @Test
    @DisplayName("빈 비밀번호를 가진 스터디룸의 응답 생성")
    void 빈_비밀번호를_가진_스터디룸의_응답_생성() {
        // given
        StudyRoom studyRoomWithEmptyPassword = StudyRoom.builder()
                .userId(789L)
                .title("빈 비밀번호 스터디룸")
                .password("   ") // 공백만 있는 비밀번호
                .maxParticipants(5)
                .category(StudyRoomCategory.CERTIFICATION)
                .build();

        // roomId 설정
        try {
            var roomIdField = StudyRoom.class.getDeclaredField("roomId");
            roomIdField.setAccessible(true);
            roomIdField.set(studyRoomWithEmptyPassword, 300L);
        } catch (Exception e) {
            throw new RuntimeException("테스트 설정 실패", e);
        }

        // when
        CreateStudyRoomResponse response = CreateStudyRoomResponse.from(studyRoomWithEmptyPassword);

        // then
        assertThat(response.isHasPassword()).isFalse(); // 공백은 비밀번호 없음으로 처리
    }

    @Test
    @DisplayName("최소 정보만 있는 스터디룸의 응답 생성")
    void 최소_정보만_있는_스터디룸의_응답_생성() {
        // given
        StudyRoom minimalStudyRoom = StudyRoom.builder()
                .userId(999L)
                .title("최소 스터디룸")
                .maxParticipants(2)
                .category(StudyRoomCategory.SELF_STUDY)
                .build();

        // roomId 설정
        try {
            var roomIdField = StudyRoom.class.getDeclaredField("roomId");
            roomIdField.setAccessible(true);
            roomIdField.set(minimalStudyRoom, 400L);
        } catch (Exception e) {
            throw new RuntimeException("테스트 설정 실패", e);
        }

        // when
        CreateStudyRoomResponse response = CreateStudyRoomResponse.from(minimalStudyRoom);

        // then
        assertThat(response.getRoomId()).isEqualTo(400L);
        assertThat(response.getTitle()).isEqualTo("최소 스터디룸");
        assertThat(response.getDescription()).isNull();
        assertThat(response.isHasPassword()).isFalse();
        assertThat(response.getMaxParticipants()).isEqualTo(2);
        assertThat(response.getTag()).isNull();
        assertThat(response.getCategory()).isEqualTo(StudyRoomCategory.SELF_STUDY);
        assertThat(response.getFocusTime()).isNull();
        assertThat(response.getBreakTime()).isNull();
        assertThat(response.getInviteHashCode()).isNull();
        assertThat(response.getOwnerId()).isEqualTo(999L);
    }

    @Test
    @DisplayName("빌더 패턴으로 직접 응답 생성")
    void 빌더_패턴으로_직접_응답_생성() {
        // given & when
        CreateStudyRoomResponse response = CreateStudyRoomResponse.builder()
                .roomId(500L)
                .title("직접 생성 스터디룸")
                .description("빌더로 직접 생성")
                .hasPassword(true)
                .maxParticipants(6)
                .tag("테스트")
                .category(StudyRoomCategory.MEETING)
                .focusTime(30)
                .breakTime(10)
                .inviteHashCode("XYZ789ABC123")
                .createdAt(testCreatedAt)
                .ownerId(888L)
                .build();

        // then
        assertThat(response.getRoomId()).isEqualTo(500L);
        assertThat(response.getTitle()).isEqualTo("직접 생성 스터디룸");
        assertThat(response.getDescription()).isEqualTo("빌더로 직접 생성");
        assertThat(response.isHasPassword()).isTrue();
        assertThat(response.getMaxParticipants()).isEqualTo(6);
        assertThat(response.getTag()).isEqualTo("테스트");
        assertThat(response.getCategory()).isEqualTo(StudyRoomCategory.MEETING);
        assertThat(response.getFocusTime()).isEqualTo(30);
        assertThat(response.getBreakTime()).isEqualTo(10);
        assertThat(response.getInviteHashCode()).isEqualTo("XYZ789ABC123");
        assertThat(response.getCreatedAt()).isEqualTo(testCreatedAt);
        assertThat(response.getOwnerId()).isEqualTo(888L);
    }

    @Test
    @DisplayName("전체 생성자로 응답 생성")
    void 전체_생성자로_응답_생성() {
        // given & when
        CreateStudyRoomResponse response = new CreateStudyRoomResponse(
                600L,
                "생성자 테스트 스터디룸",
                "전체 생성자로 생성",
                false,
                6,
                "생성자",
                StudyRoomCategory.SCHOOL_STUDY,
                45,
                15,
                "DEF456GHI789",
                testCreatedAt,
                777L
        );

        // then
        assertThat(response.getRoomId()).isEqualTo(600L);
        assertThat(response.getTitle()).isEqualTo("생성자 테스트 스터디룸");
        assertThat(response.getDescription()).isEqualTo("전체 생성자로 생성");
        assertThat(response.isHasPassword()).isFalse();
        assertThat(response.getMaxParticipants()).isEqualTo(6);
        assertThat(response.getTag()).isEqualTo("생성자");
        assertThat(response.getCategory()).isEqualTo(StudyRoomCategory.SCHOOL_STUDY);
        assertThat(response.getFocusTime()).isEqualTo(45);
        assertThat(response.getBreakTime()).isEqualTo(15);
        assertThat(response.getInviteHashCode()).isEqualTo("DEF456GHI789");
        assertThat(response.getCreatedAt()).isEqualTo(testCreatedAt);
        assertThat(response.getOwnerId()).isEqualTo(777L);
    }
}
