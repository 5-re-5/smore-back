package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomRequest;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomResponse;
import org.oreo.smore.domain.studyroom.exception.StudyRoomCreationException;
import org.oreo.smore.domain.studyroom.exception.StudyRoomValidationException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("스터디룸 생성 서비스 테스트")
public class StudyRoomCreationServiceTest {

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @InjectMocks
    private StudyRoomCreationService studyRoomCreationService;

    private CreateStudyRoomRequest validRequest;
    private StudyRoom savedStudyRoom;

    @BeforeEach
    void setUp() {
        validRequest = CreateStudyRoomRequest.builder()
                .title("Java 스터디")
                .description("매주 화요일 Java 기초 스터디")
                .password("1234")
                .maxParticipants(4)
                .tag("개발")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(25)
                .breakTime(5)
                .build();

        savedStudyRoom = StudyRoom.builder()
                .roomId(1L)
                .userId(100L)
                .title("Java 스터디")
                .description("매주 화요일 Java 기초 스터디")
                .password("1234")
                .maxParticipants(4)
                .tag("개발")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(25)
                .breakTime(5)
                .inviteHashCode("ABCD1234EFGH")
                .build();
    }

    @Test
    @DisplayName("정상적인 스터디룸 생성")
    void 정상적인_스터디룸_생성() {
        // given
        Long userId = 100L;
        when(studyRoomRepository.save(any(StudyRoom.class))).thenReturn(savedStudyRoom);

        // when
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, validRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Java 스터디");
        assertThat(response.getInviteHashCode()).isEqualTo("ABCD1234EFGH");
        verify(studyRoomRepository, times(1)).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("비밀번호 없는 스터디룸 생성")
    void 비밀번호_없는_스터디룸_생성() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest requestWithoutPassword = CreateStudyRoomRequest.builder()
                .title("공개 스터디")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        StudyRoom savedRoomWithoutPassword = StudyRoom.builder()
                .roomId(2L)
                .userId(userId)
                .title("공개 스터디")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .inviteHashCode("XYZ123456789")
                .build();

        when(studyRoomRepository.save(any(StudyRoom.class))).thenReturn(savedRoomWithoutPassword);

        // when
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, requestWithoutPassword);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(2L);
        assertThat(response.getTitle()).isEqualTo("공개 스터디");
        verify(studyRoomRepository, times(1)).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("타이머 설정 없는 스터디룸 생성")
    void 타이머_설정_없는_스터디룸_생성() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest requestWithoutTimer = CreateStudyRoomRequest.builder()
                .title("자유 스터디")
                .category(StudyRoomCategory.MEETING)
                .build();

        StudyRoom savedRoomWithoutTimer = StudyRoom.builder()
                .roomId(3L)
                .userId(userId)
                .title("자유 스터디")
                .category(StudyRoomCategory.MEETING)
                .maxParticipants(6)
                .inviteHashCode("FREE12345678")
                .build();

        when(studyRoomRepository.save(any(StudyRoom.class))).thenReturn(savedRoomWithoutTimer);

        // when
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, requestWithoutTimer);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMaxParticipants()).isEqualTo(6); // 기본값 확인
        verify(studyRoomRepository, times(1)).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("제목 null일 때 검증 실패")
    void 제목_null일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title(null)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("스터디룸 제목은 필수입니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("제목 빈 문자열일 때 검증 실패")
    void 제목_빈_문자열일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("   ")
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("스터디룸 제목은 필수입니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("제목 길이 초과 시 검증 실패")
    void 제목_길이_초과_시_검증_실패() {
        // given
        Long userId = 100L;
        String longTitle = "a".repeat(101); // 101자
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title(longTitle)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("스터디룸 제목은 100자 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("설명 길이 초과 시 검증 실패")
    void 설명_길이_초과_시_검증_실패() {
        // given
        Long userId = 100L;
        String longDescription = "a".repeat(1001); // 1001자
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .description(longDescription)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("스터디룸 설명은 1000자 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("비밀번호 길이 초과 시 검증 실패")
    void 비밀번호_길이_초과_시_검증_실패() {
        // given
        Long userId = 100L;
        String longPassword = "a".repeat(21); // 21자
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .password(longPassword)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("비밀번호는 20자 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("최대 참가자 수 최소값 미만일 때 검증 실패")
    void 최대_참가자수_최소값_미만일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .maxParticipants(0)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("최대 참가자 수는 최소 1명이어야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("최대 참가자 수 최대값 초과일 때 검증 실패")
    void 최대_참가자수_최대값_초과일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .maxParticipants(7)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("최대 참가자 수는 최대 6명까지 가능합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("태그 길이 초과 시 검증 실패")
    void 태그_길이_초과_시_검증_실패() {
        // given
        Long userId = 100L;
        String longTag = "a".repeat(51); // 51자
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .tag(longTag)
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("태그는 50자 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("카테고리 null일 때 검증 실패")
    void 카테고리_null일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(null)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("스터디룸 카테고리는 필수입니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("집중 시간만 설정된 경우 검증 실패")
    void 집중시간만_설정된_경우_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(25)
                .breakTime(null)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("집중 시간과 휴식 시간은 함께 설정되어야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("휴식 시간만 설정된 경우 검증 실패")
    void 휴식시간만_설정된_경우_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(null)
                .breakTime(5)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("집중 시간과 휴식 시간은 함께 설정되어야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("집중 시간 최소값 미만일 때 검증 실패")
    void 집중시간_최소값_미만일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(4) // 5분 미만
                .breakTime(5)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("집중 시간은 5분 이상 240분 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("집중 시간 최대값 초과일 때 검증 실패")
    void 집중시간_최대값_초과일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(241) // 240분 초과
                .breakTime(5)
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("집중 시간은 5분 이상 240분 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("휴식 시간 최소값 미만일 때 검증 실패")
    void 휴식시간_최소값_미만일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(25)
                .breakTime(4) // 5분 미만
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("휴식 시간은 5분 이상 60분 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("휴식 시간 최대값 초과일 때 검증 실패")
    void 휴식시간_최대값_초과일_때_검증_실패() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("테스트 스터디")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(25)
                .breakTime(61) // 60분 초과
                .build();

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("휴식 시간은 5분 이상 60분 이하여야 합니다.");
        verify(studyRoomRepository, never()).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("DB 저장 중 예외 발생 시 StudyRoomCreationException 발생")
    void DB_저장_중_예외_발생_시_StudyRoomCreationException_발생() {
        // given
        Long userId = 100L;
        when(studyRoomRepository.save(any(StudyRoom.class))).thenThrow(new RuntimeException("DB 연결 오류"));

        // when & then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(userId, validRequest))
                .isInstanceOf(StudyRoomCreationException.class)
                .hasMessageContaining("스터디룸 생성에 실패했습니다")
                .hasCauseInstanceOf(RuntimeException.class);
        verify(studyRoomRepository, times(1)).save(any(StudyRoom.class));
    }

    @Test
    @DisplayName("초대 해시코드가 생성되는지 확인")
    void 초대_해시코드가_생성되는지_확인() {
        // given
        Long userId = 100L;
        when(studyRoomRepository.save(any(StudyRoom.class))).thenReturn(savedStudyRoom);

        // when
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, validRequest);

        // then
        assertThat(response.getInviteHashCode()).isNotNull();
        assertThat(response.getInviteHashCode()).hasSize(12);
        assertThat(response.getInviteHashCode()).matches("[A-Z0-9]+");
    }

    @Test
    @DisplayName("문자열 필드들의 trim 처리 확인")
    void 문자열_필드들의_trim_처리_확인() {
        // given
        Long userId = 100L;
        CreateStudyRoomRequest requestWithSpaces = CreateStudyRoomRequest.builder()
                .title("  Java 스터디  ")
                .description("  설명입니다  ")
                .password("  1234  ")
                .tag("  개발  ")
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        StudyRoom expectedSavedRoom = StudyRoom.builder()
                .roomId(1L)
                .userId(userId)
                .title("Java 스터디")
                .description("설명입니다")
                .password("1234")
                .tag("개발")
                .category(StudyRoomCategory.EMPLOYMENT)
                .maxParticipants(6)
                .inviteHashCode("TRIMTEST1234")
                .build();

        when(studyRoomRepository.save(any(StudyRoom.class))).thenReturn(expectedSavedRoom);

        // when
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, requestWithSpaces);

        // then
        assertThat(response.getTitle()).isEqualTo("Java 스터디");
        verify(studyRoomRepository, times(1)).save(any(StudyRoom.class));
    }
}
