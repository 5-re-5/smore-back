package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.dto.UpdatePersonalStatusResponse;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantService - 개인 상태 변경 단위 테스트")
class ParticipantServiceUpdateTest {

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
    private User mockUser;
    private Participant mockParticipant;

    @BeforeEach
    void setUp() {
        // 방 정보 설정
        mockStudyRoom = StudyRoom.builder()
                .roomId(1L)
                .userId(100L)
                .title("테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .isAllMuted(false)
                .build();

        // 사용자 정보 설정
        mockUser = User.builder()
                .userId(100L)
                .nickname("김철수")
                .email("test@test.com")
                .goalStudyTime(300)
                .build();

        // 참가자 정보 설정 (초기 상태: 마이크 끄고 카메라 켜짐)
        mockParticipant = Participant.builder()
                .roomId(1L)
                .userId(100L)
                .build();

        // 초기 미디어 상태 설정
        mockParticipant.updateMediaStatus(false, true, "테스트");
    }

    @Test
    @DisplayName("개인 미디어 상태 변경 성공 - 오디오/비디오 동시 변경")
    void updatePersonalMediaStatus_Success() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Boolean audioEnabled = true;
        Boolean videoEnabled = false;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // When
        UpdatePersonalStatusResponse response = participantService.updatePersonalMediaStatus(
                roomId, userId, audioEnabled, videoEnabled);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getNickname()).isEqualTo("김철수");
        assertThat(response.getAudioEnabled()).isTrue();
        assertThat(response.getVideoEnabled()).isFalse();
        assertThat(response.getMessage()).isEqualTo("개인 미디어 상태가 성공적으로 변경되었습니다");

        // 실제 참가자 상태 변경 확인
        assertThat(mockParticipant.isAudioEnabled()).isTrue();
        assertThat(mockParticipant.isVideoEnabled()).isFalse();

        // Mock 호출 검증
        verify(studyRoomRepository).findById(roomId);
        verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("개인 오디오 상태만 변경 성공")
    void updatePersonalAudioStatus_Success() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Boolean audioEnabled = true;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 초기 상태 확인 (오디오: false, 비디오: true)
        assertThat(mockParticipant.isAudioEnabled()).isFalse();
        assertThat(mockParticipant.isVideoEnabled()).isTrue();

        // When
        UpdatePersonalStatusResponse response = participantService.updatePersonalAudioStatus(
                roomId, userId, audioEnabled);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAudioEnabled()).isTrue();
        assertThat(response.getVideoEnabled()).isTrue(); // 비디오 상태는 유지

        // 실제 참가자 상태 확인
        assertThat(mockParticipant.isAudioEnabled()).isTrue();  // 변경됨
        assertThat(mockParticipant.isVideoEnabled()).isTrue();  // 유지됨
    }

    @Test
    @DisplayName("개인 비디오 상태만 변경 성공")
    void updatePersonalVideoStatus_Success() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Boolean videoEnabled = false;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 초기 상태 확인 (오디오: false, 비디오: true)
        assertThat(mockParticipant.isAudioEnabled()).isFalse();
        assertThat(mockParticipant.isVideoEnabled()).isTrue();

        // When
        UpdatePersonalStatusResponse response = participantService.updatePersonalVideoStatus(
                roomId, userId, videoEnabled);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAudioEnabled()).isFalse(); // 오디오 상태는 유지
        assertThat(response.getVideoEnabled()).isFalse();

        // 실제 참가자 상태 확인
        assertThat(mockParticipant.isAudioEnabled()).isFalse(); // 유지됨
        assertThat(mockParticipant.isVideoEnabled()).isFalse(); // 변경됨
    }

    @Test
    @DisplayName("마이크 상태 토글 성공 - false → true")
    void toggleAudioStatus_Success_FalseToTrue() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 초기 상태: 마이크 꺼짐
        assertThat(mockParticipant.isAudioEnabled()).isFalse();

        // When
        UpdatePersonalStatusResponse response = participantService.toggleAudioStatus(roomId, userId);

        // Then
        assertThat(response.getAudioEnabled()).isTrue(); // 토글로 켜짐
        assertThat(mockParticipant.isAudioEnabled()).isTrue();
    }

    @Test
    @DisplayName("카메라 상태 토글 성공 - true → false")
    void toggleVideoStatus_Success_TrueToFalse() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 초기 상태: 카메라 켜짐
        assertThat(mockParticipant.isVideoEnabled()).isTrue();

        // When
        UpdatePersonalStatusResponse response = participantService.toggleVideoStatus(roomId, userId);

        // Then
        assertThat(response.getVideoEnabled()).isFalse(); // 토글로 꺼짐
        assertThat(mockParticipant.isVideoEnabled()).isFalse();
    }

    @Test
    @DisplayName("상태 변경 실패 - 존재하지 않는 방")
    void updatePersonalMediaStatus_Fail_RoomNotFound() {
        // Given
        Long roomId = 999L;
        Long userId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participantService.updatePersonalMediaStatus(
                roomId, userId, true, false))
                .isInstanceOf(ParticipantException.StudyRoomNotFoundException.class)
                .hasMessageContaining("방 999를 찾을 수 없습니다");

        verify(studyRoomRepository).findById(roomId);
        verifyNoInteractions(participantRepository, userRepository);
    }

    @Test
    @DisplayName("상태 변경 실패 - 참가자 없음")
    void updatePersonalMediaStatus_Fail_ParticipantNotFound() {
        // Given
        Long roomId = 1L;
        Long userId = 999L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of()); // 빈 리스트

        // When & Then
        assertThatThrownBy(() -> participantService.updatePersonalMediaStatus(
                roomId, userId, true, false))
                .isInstanceOf(ParticipantException.ParticipantNotFoundException.class)
                .hasMessageContaining("방 1에서 사용자 999를 찾을 수 없습니다");

        verify(studyRoomRepository).findById(roomId);
        verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("상태 변경 실패 - 사용자 정보 없음")
    void updatePersonalMediaStatus_Fail_UserNotFound() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participantService.updatePersonalMediaStatus(
                roomId, userId, true, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다: 100");

        verify(studyRoomRepository).findById(roomId);
        verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("미디어 상태 변경 - 다양한 조합 테스트")
    void updatePersonalMediaStatus_VariousCombinations() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 1. 둘 다 켜기
        UpdatePersonalStatusResponse response1 = participantService.updatePersonalMediaStatus(
                roomId, userId, true, true);
        assertThat(response1.getAudioEnabled()).isTrue();
        assertThat(response1.getVideoEnabled()).isTrue();

        // 2. 둘 다 끄기
        UpdatePersonalStatusResponse response2 = participantService.updatePersonalMediaStatus(
                roomId, userId, false, false);
        assertThat(response2.getAudioEnabled()).isFalse();
        assertThat(response2.getVideoEnabled()).isFalse();

        // 3. 마이크만 켜기
        UpdatePersonalStatusResponse response3 = participantService.updatePersonalMediaStatus(
                roomId, userId, true, false);
        assertThat(response3.getAudioEnabled()).isTrue();
        assertThat(response3.getVideoEnabled()).isFalse();

        // 4. 카메라만 켜기
        UpdatePersonalStatusResponse response4 = participantService.updatePersonalMediaStatus(
                roomId, userId, false, true);
        assertThat(response4.getAudioEnabled()).isFalse();
        assertThat(response4.getVideoEnabled()).isTrue();
    }

    @Test
    @DisplayName("연속 토글 테스트 - 마이크 on/off 반복")
    void toggleAudioStatus_Multiple_Times() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        when(participantRepository.findActiveParticipantsByRoomId(roomId))
                .thenReturn(List.of(mockParticipant));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 초기 상태: false
        assertThat(mockParticipant.isAudioEnabled()).isFalse();

        // 첫 번째 토글: false → true
        UpdatePersonalStatusResponse response1 = participantService.toggleAudioStatus(roomId, userId);
        assertThat(response1.getAudioEnabled()).isTrue();

        // 두 번째 토글: true → false
        UpdatePersonalStatusResponse response2 = participantService.toggleAudioStatus(roomId, userId);
        assertThat(response2.getAudioEnabled()).isFalse();

        // 세 번째 토글: false → true
        UpdatePersonalStatusResponse response3 = participantService.toggleAudioStatus(roomId, userId);
        assertThat(response3.getAudioEnabled()).isTrue();
    }
}