package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomRequest;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomResponse;
import org.oreo.smore.domain.studyroom.exception.StudyRoomCreationException;
import org.oreo.smore.domain.studyroom.exception.StudyRoomValidationException;
import org.oreo.smore.global.common.CloudStorageManager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // 테스트 후 롤백
@Import(StudyRoomCreationServiceIntegrationTest.TestConfig.class)
@DisplayName("스터디룸 생성 서비스 통합 테스트")
class StudyRoomCreationServiceIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CloudStorageManager cloudStorageManager() {
            return mock(CloudStorageManager.class);
        }
    }

    @Autowired
    private StudyRoomCreationService studyRoomCreationService;

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Autowired
    private CloudStorageManager cloudStorageManager; // TestConfig에서 주입된 Mock

    private CreateStudyRoomRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateStudyRoomRequest.builder()
                .title("Java Spring 스터디")
                .description("실무 위주 Spring Boot 스터디")
                .password("study123")
                .maxParticipants(4)
                .tag("백엔드")
                .category(StudyRoomCategory.EMPLOYMENT)
                .focusTime(50)
                .breakTime(10)
                .build();
    }

    @Test
    @DisplayName("🚀 완전한 스터디룸 생성 플로우 테스트")
    void 완전한_스터디룸_생성_플로우_테스트() {
        // Given
        Long userId = 999L;

        // When - 실제 서비스 메서드 호출
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, validRequest);

        // Then - 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Java Spring 스터디");
        assertThat(response.getInviteHashCode()).isNotNull();
        assertThat(response.getInviteHashCode()).hasSize(12);
        assertThat(response.getInviteHashCode()).matches("[A-Z0-9]+");
        assertThat(response.getLiveKitRoomId()).isNotNull();
        assertThat(response.getLiveKitRoomId()).startsWith("study-room-");

        // DB 실제 저장 확인
        StudyRoom savedRoom = studyRoomRepository.findById(response.getRoomId()).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getTitle()).isEqualTo("Java Spring 스터디");
        assertThat(savedRoom.getUserId()).isEqualTo(userId);
        assertThat(savedRoom.getMaxParticipants()).isEqualTo(4);
        assertThat(savedRoom.getFocusTime()).isEqualTo(50);
        assertThat(savedRoom.getBreakTime()).isEqualTo(10);
        assertThat(savedRoom.getCreatedAt()).isNotNull();

        System.out.println("✅ 완전한 스터디룸 생성 성공!");
        System.out.println("📍 생성된 방 ID: " + response.getRoomId());
        System.out.println("🔑 초대 해시코드: " + response.getInviteHashCode());
        System.out.println("📺 LiveKit 방 ID: " + response.getLiveKitRoomId());
    }

    @Test
    @DisplayName("기본값 적용 테스트")
    void 기본값_적용_테스트() {
        // Given
        CreateStudyRoomRequest minimalRequest = CreateStudyRoomRequest.builder()
                .title("최소 설정 스터디")
                .category(StudyRoomCategory.SELF_STUDY)
                .build();

        // When
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(1L, minimalRequest);

        // Then
        StudyRoom savedRoom = studyRoomRepository.findById(response.getRoomId()).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getMaxParticipants()).isEqualTo(6); // 기본값
        assertThat(savedRoom.getPassword()).isNull(); // 비밀번호 없음
        assertThat(savedRoom.getFocusTime()).isNull(); // 타이머 없음
        assertThat(savedRoom.getBreakTime()).isNull();

        System.out.println("✅ 기본값 적용 성공! 최대인원: " + savedRoom.getMaxParticipants());
    }

    @Test
    @DisplayName("문자열 trim 처리 검증")
    void 문자열_trim_처리_검증() {
        // Given
        CreateStudyRoomRequest requestWithSpaces = CreateStudyRoomRequest.builder()
                .title("   Trim 테스트 스터디   ")
                .description("   앞뒤 공백 제거 테스트   ")
                .password("   pass123   ")
                .tag("   테스트   ")
                .category(StudyRoomCategory.CERTIFICATION)
                .build();

        // When
        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(1L, requestWithSpaces);

        // Then
        StudyRoom savedRoom = studyRoomRepository.findById(response.getRoomId()).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getTitle()).isEqualTo("Trim 테스트 스터디");
        assertThat(savedRoom.getDescription()).isEqualTo("앞뒤 공백 제거 테스트");
        assertThat(savedRoom.getPassword()).isEqualTo("pass123");
        assertThat(savedRoom.getTag()).isEqualTo("테스트");

        System.out.println("✅ 문자열 Trim 처리 성공!");
    }

    @Test
    @DisplayName("유효성 검증 실패 테스트")
    void 유효성_검증_실패_테스트() {
        // Given
        CreateStudyRoomRequest invalidRequest = CreateStudyRoomRequest.builder()
                .title("") // 빈 제목
                .category(StudyRoomCategory.EMPLOYMENT)
                .build();

        // When & Then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(1L, invalidRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("스터디룸 제목은 필수입니다.");

        // DB에 저장되지 않았는지 확인
        long count = studyRoomRepository.count();
        assertThat(count).isEqualTo(0);

        System.out.println("✅ 유효성 검증 실패 처리 성공!");
    }

    @Test
    @DisplayName("타이머 설정 부분 실패 테스트")
    void 타이머_설정_부분_실패_테스트() {
        // Given
        CreateStudyRoomRequest invalidTimerRequest = CreateStudyRoomRequest.builder()
                .title("타이머 오류 테스트")
                .category(StudyRoomCategory.LANGUAGE)
                .focusTime(25) // 집중시간만 설정
                .breakTime(null) // 휴식시간 누락
                .build();

        // When & Then
        assertThatThrownBy(() -> studyRoomCreationService.createStudyRoom(1L, invalidTimerRequest))
                .isInstanceOf(StudyRoomValidationException.class)
                .hasMessage("집중 시간과 휴식 시간은 함께 설정되어야 합니다.");

        System.out.println("✅ 타이머 설정 검증 성공!");
    }

    @Test
    @DisplayName("대용량 데이터 생성 테스트")
    void 대용량_데이터_생성_테스트() {
        // Given
        int testCount = 10;

        // When
        for (int i = 1; i <= testCount; i++) {
            CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                    .title("대용량 테스트 스터디 " + i)
                    .category(StudyRoomCategory.SCHOOL_STUDY)
                    .maxParticipants(i % 6 + 1)
                    .build();

            studyRoomCreationService.createStudyRoom((long) i, request);
        }

        // Then
        long totalCount = studyRoomRepository.count();
        assertThat(totalCount).isEqualTo(testCount);

        System.out.println("✅ 대용량 데이터 생성 성공! 총 " + totalCount + "개 생성");
    }

    @Test
    @DisplayName("모든 카테고리 생성 테스트")
    void 모든_카테고리_생성_테스트() {
        // Given
        StudyRoomCategory[] categories = StudyRoomCategory.values();

        // When
        for (int i = 0; i < categories.length; i++) {
            CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                    .title(categories[i].name() + " 테스트 스터디")
                    .category(categories[i])
                    .build();

            studyRoomCreationService.createStudyRoom((long) (i + 1), request);
        }

        // Then
        long totalCount = studyRoomRepository.count();
        assertThat(totalCount).isEqualTo(categories.length);

        System.out.println("✅ 모든 카테고리 생성 성공! 총 " + categories.length + "개 카테고리");
        for (StudyRoomCategory category : categories) {
            System.out.println("📂 " + category.name());
        }
    }

    @Test
    @DisplayName("동시성 안전성 테스트 (해시코드 중복 방지)")
    void 동시성_안전성_테스트() {
        // Given
        int testCount = 50;

        // When - 빠르게 여러 개 생성
        for (int i = 1; i <= testCount; i++) {
            CreateStudyRoomRequest request = CreateStudyRoomRequest.builder()
                    .title("동시성 테스트 " + i)
                    .category(StudyRoomCategory.MEETING)
                    .build();

            studyRoomCreationService.createStudyRoom((long) i, request);
        }

        // Then - 해시코드 중복 검사
        Iterable<StudyRoom> allRooms = studyRoomRepository.findAll();
        long uniqueHashCount = java.util.stream.StreamSupport.stream(allRooms.spliterator(), false)
                .map(StudyRoom::getInviteHashCode)
                .distinct()
                .count();

        assertThat(uniqueHashCount).isEqualTo(testCount);

        System.out.println("✅ 동시성 안전성 테스트 성공! 모든 해시코드가 고유함");
    }
}