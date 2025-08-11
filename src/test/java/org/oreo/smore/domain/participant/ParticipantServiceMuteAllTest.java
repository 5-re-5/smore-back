package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.dto.MuteAllResponse;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantService - 전체 음소거/해제 단위 테스트")
class ParticipantServiceMuteAllTest {

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
    private Participant mockOwner;      // 방장
    private Participant mockParticipant1; // 일반 참가자 1
    private Participant mockParticipant2; // 일반 참가자 2

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

        // 방장 참가자 정보 (오디오 켜져있음)
        mockOwner = createMockParticipant(1L, 100L, true, true);

        // 일반 참가자 1 (오디오 켜져있음)
        mockParticipant1 = createMockParticipant(1L, 200L, true, false);

        // 일반 참가자 2 (오디오 꺼져있음 - 이미 음소거)
        mockParticipant2 = createMockParticipant(1L, 300L, false, true);
    }

    // ==================== 전체 음소거 설정 테스트 ====================

    @Test
    @DisplayName("전체 음소거 설정 성공 - 방장이 모든 참가자 음소거")
    void muteAllParticipants_Success() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant1, mockParticipant2));

        // When
        MuteAllResponse response = participantService.muteAllParticipants(roomId, ownerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1L);
        assertThat(response.getIsAllMuted()).isTrue();
        assertThat(response.getTotalParticipants()).isEqualTo(3);
        assertThat(response.getMutedParticipants()).isEqualTo(1); // 방장 제외, 이미 음소거된 참가자 제외
        assertThat(response.getPerformedBy()).isEqualTo(100L);
        assertThat(response.getMessage()).contains("전체 음소거가 설정되었습니다");

        // 방장은 음소거되지 않음
        assertThat(mockOwner.isAudioEnabled()).isTrue();

        // 일반 참가자1은 음소거됨
        assertThat(mockParticipant1.isAudioEnabled()).isFalse();

        // 일반 참가자2는 이미 음소거 상태 유지
        assertThat(mockParticipant2.isAudioEnabled()).isFalse();

        // StudyRoom 상태 업데이트 확인
        assertThat(mockStudyRoom.isAllMuted()).isTrue();

        verify(studyRoomRepository).save(mockStudyRoom);
    }

    @Test
    @DisplayName("전체 음소거 설정 실패 - 방장 권한 없음")
    void muteAllParticipants_Fail_NotOwner() {
        // Given
        Long roomId = 1L;
        Long notOwnerId = 999L; // 방장이 아닌 사용자

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));

        // When & Then
        assertThatThrownBy(() -> participantService.muteAllParticipants(roomId, notOwnerId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("방장만 전체 음소거를 설정/해제할 수 있습니다");

        verify(studyRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("전체 음소거 설정 실패 - 이미 전체 음소거 상태")
    void muteAllParticipants_Fail_AlreadyMuted() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 이미 전체 음소거 상태
        mockStudyRoom.enableAllMute();

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));

        // When & Then
        assertThatThrownBy(() -> participantService.muteAllParticipants(roomId, ownerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 전체 음소거 상태입니다");
    }

    @Test
    @DisplayName("전체 음소거 설정 실패 - 참가자 없음")
    void muteAllParticipants_Fail_NoParticipants() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of()); // 빈 리스트

        // When & Then
        assertThatThrownBy(() -> participantService.muteAllParticipants(roomId, ownerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("참가자가 없어 전체 음소거를 설정할 수 없습니다");
    }

    @Test
    @DisplayName("전체 음소거 설정 - 방장만 있는 경우")
    void muteAllParticipants_OnlyOwner() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner)); // 방장만

        // When
        MuteAllResponse response = participantService.muteAllParticipants(roomId, ownerId);

        // Then
        assertThat(response.getTotalParticipants()).isEqualTo(1);
        assertThat(response.getMutedParticipants()).isEqualTo(0); // 방장은 음소거 안됨
        assertThat(mockOwner.isAudioEnabled()).isTrue(); // 방장은 여전히 오디오 켜져있음
    }

    // ==================== 전체 음소거 해제 테스트 ====================

    @Test
    @DisplayName("전체 음소거 해제 성공 - 전체 음소거 상태에서 해제")
    void unmuteAllParticipants_Success_FromAllMuted() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 전체 음소거 상태로 설정
        mockStudyRoom.enableAllMute();

        // 모든 참가자 음소거 상태로 설정 (방장 제외)
        mockOwner.updateMediaStatus(true, true, "테스트");     // 방장은 오디오 켜짐
        mockParticipant1.updateMediaStatus(false, false, "테스트"); // 음소거됨
        mockParticipant2.updateMediaStatus(false, true, "테스트");  // 음소거됨

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant1, mockParticipant2));

        // When
        MuteAllResponse response = participantService.unmuteAllParticipants(roomId, ownerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo(1L);
        assertThat(response.getIsAllMuted()).isFalse();
        assertThat(response.getTotalParticipants()).isEqualTo(3);
        assertThat(response.getUnmutedParticipants()).isEqualTo(2); // 음소거된 2명 해제
        assertThat(response.getPerformedBy()).isEqualTo(100L);
        assertThat(response.getMessage()).contains("전체 음소거가 해제되었습니다");

        // 모든 참가자 음소거 해제됨
        assertThat(mockOwner.isAudioEnabled()).isTrue();
        assertThat(mockParticipant1.isAudioEnabled()).isTrue();
        assertThat(mockParticipant2.isAudioEnabled()).isTrue();

        // StudyRoom 상태 업데이트 확인
        assertThat(mockStudyRoom.isAllMuted()).isFalse();

        verify(studyRoomRepository).save(mockStudyRoom);
    }

    @Test
    @DisplayName("전체 음소거 해제 성공 - 전체 음소거 아닌 상태에서도 개별 음소거 해제")
    void unmuteAllParticipants_Success_NotAllMutedButIndividualMuted() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 전체 음소거 상태 아님 (기본값 false)
        assertThat(mockStudyRoom.isAllMuted()).isFalse();

        // 일부 참가자는 개별적으로 음소거됨
        mockOwner.updateMediaStatus(true, true, "테스트");     // 방장 오디오 켜짐
        mockParticipant1.updateMediaStatus(false, false, "테스트"); // 개별 음소거
        mockParticipant2.updateMediaStatus(false, true, "테스트");  // 개별 음소거

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant1, mockParticipant2));

        // When
        MuteAllResponse response = participantService.unmuteAllParticipants(roomId, ownerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIsAllMuted()).isFalse();
        assertThat(response.getUnmutedParticipants()).isEqualTo(2); // 개별 음소거된 2명 해제
        assertThat(response.getMessage()).contains("전체 음소거가 해제되었습니다");

        // 모든 참가자 음소거 해제됨
        assertThat(mockParticipant1.isAudioEnabled()).isTrue();
        assertThat(mockParticipant2.isAudioEnabled()).isTrue();

        verify(studyRoomRepository).save(mockStudyRoom);
    }

    @Test
    @DisplayName("전체 음소거 해제 - 빈 방 처리")
    void unmuteAllParticipants_EmptyRoom() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 전체 음소거 상태
        mockStudyRoom.enableAllMute();

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of()); // 빈 방

        // When
        MuteAllResponse response = participantService.unmuteAllParticipants(roomId, ownerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIsAllMuted()).isFalse();
        assertThat(response.getTotalParticipants()).isEqualTo(0);
        assertThat(response.getUnmutedParticipants()).isEqualTo(0);
        assertThat(response.getMessage()).contains("참가자 없음");

        // 전체 음소거 상태는 해제됨
        assertThat(mockStudyRoom.isAllMuted()).isFalse();
        verify(studyRoomRepository).save(mockStudyRoom);
    }

    @Test
    @DisplayName("전체 음소거 해제 실패 - 방장 권한 없음")
    void unmuteAllParticipants_Fail_NotOwner() {
        // Given
        Long roomId = 1L;
        Long notOwnerId = 999L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));

        // When & Then
        assertThatThrownBy(() -> participantService.unmuteAllParticipants(roomId, notOwnerId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("방장만 전체 음소거를 설정/해제할 수 있습니다");
    }

    // ==================== 전체 음소거 토글 테스트 ====================

    @Test
    @DisplayName("전체 음소거 토글 - false → true")
    void toggleMuteAll_FalseToTrue() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 전체 음소거 OFF 상태
        assertThat(mockStudyRoom.isAllMuted()).isFalse();

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant1));

        // When
        MuteAllResponse response = participantService.toggleMuteAll(roomId, ownerId);

        // Then
        assertThat(response.getIsAllMuted()).isTrue(); // 음소거로 토글됨
        assertThat(response.getMutedParticipants()).isNotNull(); // 음소거된 참가자 수 포함
    }

    @Test
    @DisplayName("전체 음소거 토글 - true → false")
    void toggleMuteAll_TrueToFalse() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 전체 음소거 ON 상태
        mockStudyRoom.enableAllMute();

        // 참가자들 음소거 상태로 설정
        mockParticipant1.updateMediaStatus(false, false, "테스트");

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant1));

        // When
        MuteAllResponse response = participantService.toggleMuteAll(roomId, ownerId);

        // Then
        assertThat(response.getIsAllMuted()).isFalse(); // 해제로 토글됨
        assertThat(response.getUnmutedParticipants()).isNotNull(); // 해제된 참가자 수 포함
    }

    // ==================== 예외 상황 테스트 ====================

    @Test
    @DisplayName("전체 음소거 실패 - 존재하지 않는 방")
    void muteAllParticipants_Fail_RoomNotFound() {
        // Given
        Long roomId = 999L;
        Long ownerId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participantService.muteAllParticipants(roomId, ownerId))
                .isInstanceOf(ParticipantException.StudyRoomNotFoundException.class)
                .hasMessageContaining("방 999를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("전체 음소거 해제 - 이미 모든 참가자가 음소거 해제된 상태")
    void unmuteAllParticipants_AllAlreadyUnmuted() {
        // Given
        Long roomId = 1L;
        Long ownerId = 100L;

        // 모든 참가자 이미 음소거 해제 상태
        mockOwner.updateMediaStatus(true, true, "테스트");
        mockParticipant1.updateMediaStatus(true, false, "테스트");
        mockParticipant2.updateMediaStatus(true, true, "테스트");

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockOwner, mockParticipant1, mockParticipant2));

        // When
        MuteAllResponse response = participantService.unmuteAllParticipants(roomId, ownerId);

        // Then
        assertThat(response.getUnmutedParticipants()).isEqualTo(0); // 해제할 참가자 없음
        assertThat(response.getMessage()).contains("전체 음소거가 해제되었습니다 (0명 해제)");
    }

    // ========== 헬퍼 메서드 ==========

    private Participant createMockParticipant(Long roomId, Long userId, boolean audioEnabled, boolean videoEnabled) {
        Participant participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        participant.updateMediaStatus(audioEnabled, videoEnabled, "테스트");
        return participant;
    }
}
