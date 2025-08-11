package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.dto.ParticipantStatusResponse;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studytime.StudyTime;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantService - 통합 참가자 상태 조회 테스트")
class ParticipantServiceStatusTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyTimeRepository studyTimeRepository;

    @InjectMocks
    private ParticipantService participantService;

    private StudyRoom mockStudyRoom;
    private User mockOwnerUser;
    private User mockParticipantUser;
    private Participant mockOwner;
    private Participant mockParticipant;

    @BeforeEach
    void setUp() {
        // 방 정보 설정 (전체 음소거 비활성화)
        mockStudyRoom = StudyRoom.builder()
                .roomId(1L)
                .userId(100L)  // 방장 ID
                .title("테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .isAllMuted(false)
                .build();

        // 방장 사용자 정보
        mockOwnerUser = User.builder()
                .userId(100L)
                .nickname("방장김철수")
                .email("owner@test.com")
                .goalStudyTime(300)  // 목표 5시간 (분 단위)
                .build();

        // 일반 참가자 사용자 정보
        mockParticipantUser = User.builder()
                .userId(200L)
                .nickname("참가자이영희")
                .email("participant@test.com")
                .goalStudyTime(240)  // 목표 4시간 (분 단위)
                .build();

        // 방장 참가자 정보 (마이크/카메라 모두 켜짐)
        mockOwner = createMockParticipant(1L, 100L, true, true);

        // 일반 참가자 정보 (마이크 끄고 카메라 켜짐)
        mockParticipant = createMockParticipant(1L, 200L, false, true);
    }

    @Test
    @DisplayName("참가자 상태 조회 성공 - 방장 + 일반 참가자")
    void getParticipantStatus_Success_WithOwnerAndParticipant() {
        // Given
        Long roomId = 1L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant));

        when(userRepository.findById(100L)).thenReturn(Optional.of(mockOwnerUser));
        when(userRepository.findById(200L)).thenReturn(Optional.of(mockParticipantUser));

        // 공부시간 Mock 데이터
        when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(eq(100L), any(), any()))
                .thenReturn(createMockStudyTimes(180)); // 방장: 3시간
        when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(eq(200L), any(), any()))
                .thenReturn(createMockStudyTimes(120)); // 참가자: 2시간

        // When
        ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getParticipants()).hasSize(2);
        assertThat(response.getRoomInfo().getTotalParticipants()).isEqualTo(2);
        assertThat(response.getRoomInfo().getIsAllMuted()).isFalse();

        // 방장 정보 검증
        var owner = response.getParticipants().stream()
                .filter(p -> p.getIsOwner())
                .findFirst().orElse(null);

        assertThat(owner).isNotNull();
        assertThat(owner.getUserId()).isEqualTo(100L);
        assertThat(owner.getNickname()).isEqualTo("방장김철수");
        assertThat(owner.getIsOwner()).isTrue();
        assertThat(owner.getAudioEnabled()).isTrue();
        assertThat(owner.getVideoEnabled()).isTrue();
        assertThat(owner.getTodayStudyTime()).isEqualTo(180);
        assertThat(owner.getTargetStudyTime()).isEqualTo(300);

        // 일반 참가자 정보 검증
        var participant = response.getParticipants().stream()
                .filter(p -> !p.getIsOwner())
                .findFirst().orElse(null);

        assertThat(participant).isNotNull();
        assertThat(participant.getUserId()).isEqualTo(200L);
        assertThat(participant.getNickname()).isEqualTo("참가자이영희");
        assertThat(participant.getIsOwner()).isFalse();
        assertThat(participant.getAudioEnabled()).isFalse();
        assertThat(participant.getVideoEnabled()).isTrue();
        assertThat(participant.getTodayStudyTime()).isEqualTo(120);
        assertThat(participant.getTargetStudyTime()).isEqualTo(240);

        // Mock 호출 검증
        verify(studyRoomRepository).findById(roomId);
        verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        verify(userRepository).findById(100L);
        verify(userRepository).findById(200L);
        verify(studyTimeRepository, times(2)).findAllByUserIdAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    @DisplayName("참가자 상태 조회 - 빈 방 (참가자 없음)")
    void getParticipantStatus_EmptyRoom() {
        // Given
        Long roomId = 1L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of()); // 빈 리스트

        // When
        ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getParticipants()).isEmpty();
        assertThat(response.getRoomInfo().getTotalParticipants()).isEqualTo(0);
        assertThat(response.getRoomInfo().getIsAllMuted()).isFalse();

        verify(studyRoomRepository).findById(roomId);
        verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        verifyNoInteractions(userRepository, studyTimeRepository);
    }

    @Test
    @DisplayName("참가자 상태 조회 - 전체 음소거 활성화된 방")
    void getParticipantStatus_AllMutedRoom() {
        // Given
        Long roomId = 1L;

        // 전체 음소거 활성화
        mockStudyRoom.enableAllMute();

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner));

        when(userRepository.findById(100L)).thenReturn(Optional.of(mockOwnerUser));
        when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(eq(100L), any(), any()))
                .thenReturn(createMockStudyTimes(60)); // 1시간

        // When
        ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getParticipants()).hasSize(1);
        assertThat(response.getRoomInfo().getIsAllMuted()).isTrue(); // 전체 음소거 확인
        assertThat(response.getRoomInfo().getTotalParticipants()).isEqualTo(1);
    }

    @Test
    @DisplayName("참가자 상태 조회 실패 - 존재하지 않는 방")
    void getParticipantStatus_RoomNotFound() {
        // Given
        Long roomId = 999L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participantService.getParticipantStatus(roomId))
                .isInstanceOf(ParticipantException.StudyRoomNotFoundException.class)
                .hasMessageContaining("방 999를 찾을 수 없습니다");

        verify(studyRoomRepository).findById(roomId);
        verifyNoInteractions(participantRepository, userRepository, studyTimeRepository);
    }

    @Test
    @DisplayName("참가자 상태 조회 실패 - 사용자 정보 없음")
    void getParticipantStatus_UserNotFound() {
        // Given
        Long roomId = 1L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner));

        when(userRepository.findById(100L)).thenReturn(Optional.empty()); // 사용자 없음

        // When & Then
        assertThatThrownBy(() -> participantService.getParticipantStatus(roomId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다: 100");

        verify(studyRoomRepository).findById(roomId);
        verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        verify(userRepository).findById(100L);
    }

    @Test
    @DisplayName("공부시간 계산 - 진행 중인 세션 포함")
    void getParticipantStatus_WithOngoingStudySession() {
        // Given
        Long roomId = 1L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner));
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockOwnerUser));

        // 진행 중인 공부 세션 (deletedAt이 null)
        List<StudyTime> ongoingSession = List.of(
                createStudyTime(LocalDateTime.now().minusHours(2), null) // 2시간 전부터 진행 중
        );
        when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(eq(100L), any(), any()))
                .thenReturn(ongoingSession);

        // When
        ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

        // Then
        var participant = response.getParticipants().get(0);
        assertThat(participant.getTodayStudyTime()).isGreaterThanOrEqualTo(120); // 최소 2시간
    }

    @Test
    @DisplayName("공부시간 계산 - 여러 세션 합계")
    void getParticipantStatus_MultipleStudySessions() {
        // Given
        Long roomId = 1L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner));
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockOwnerUser));

        // 여러 공부 세션
        List<StudyTime> multipleSessions = List.of(
                createStudyTime(LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(4)), // 1시간
                createStudyTime(LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2)), // 1시간
                createStudyTime(LocalDateTime.now().minusHours(1), LocalDateTime.now())                // 1시간
        );
        when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(eq(100L), any(), any()))
                .thenReturn(multipleSessions);

        // When
        ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

        // Then
        var participant = response.getParticipants().get(0);
        assertThat(participant.getTodayStudyTime()).isEqualTo(180); // 3시간 (3 * 60분)
    }

    @Test
    @DisplayName("공부시간 계산 오류 시 0분 반환")
    void getParticipantStatus_StudyTimeError() {
        // Given
        Long roomId = 1L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner));
        when(userRepository.findById(100L)).thenReturn(Optional.of(mockOwnerUser));

        // 공부시간 조회 시 예외 발생
        when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(eq(100L), any(), any()))
                .thenThrow(new RuntimeException("DB 연결 오류"));

        // When
        ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

        // Then
        var participant = response.getParticipants().get(0);
        assertThat(participant.getTodayStudyTime()).isEqualTo(0); // 오류 시 0분 반환
        assertThat(participant.getTargetStudyTime()).isEqualTo(300); // 목표시간은 정상
    }

    // ========== 헬퍼 메서드들 ==========

    private Participant createMockParticipant(Long roomId, Long userId, boolean audioEnabled, boolean videoEnabled) {
        Participant participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        participant.updateMediaStatus(audioEnabled, videoEnabled, "테스트");
        return participant;
    }

    private List<StudyTime> createMockStudyTimes(int totalMinutes) {
        LocalDateTime start = LocalDateTime.now().minusMinutes(totalMinutes);
        LocalDateTime end = LocalDateTime.now();

        return List.of(createStudyTime(start, end));
    }

    private StudyTime createStudyTime(LocalDateTime createdAt, LocalDateTime deletedAt) {
        return StudyTime.builder()
                .userId(100L)
                .createdAt(createdAt)
                .deletedAt(deletedAt)
                .build();
    }
}