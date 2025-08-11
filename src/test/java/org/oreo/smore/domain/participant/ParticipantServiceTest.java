package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.dto.ParticipantInfo;
import org.oreo.smore.domain.participant.dto.ParticipantStatusResponse;
import org.oreo.smore.domain.participant.dto.RoomInfo;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantService 단위 테스트")
class ParticipantServiceTest {

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

    @Nested
    @DisplayName("getParticipantStatus(roomId)")
    class GetParticipantStatus {

        @Test
        @DisplayName("성공: 방장 1명 + 참가자 1명 → DTO 매핑 및 방 정보 반환")
        void 성공_두명_상태조회() {
            // given
            Long roomId = 1L;

            // StudyRoom mock
            StudyRoom room = mock(StudyRoom.class);
            when(room.getUserId()).thenReturn(100L);      // 방장 = 100
            when(room.isAllMuted()).thenReturn(false);
            when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

            // Participants (participant 엔티티는 실제 객체 대신 mock으로 충분)
            Participant owner = mock(Participant.class);
            when(owner.getUserId()).thenReturn(100L);
            when(owner.isAudioEnabled()).thenReturn(true);
            when(owner.isVideoEnabled()).thenReturn(true);

            Participant member = mock(Participant.class);
            when(member.getUserId()).thenReturn(200L);
            when(member.isAudioEnabled()).thenReturn(false);
            when(member.isVideoEnabled()).thenReturn(true);

            when(participantRepository.findActiveParticipantsByRoomId(roomId))
                    .thenReturn(List.of(owner, member));

            // Users (닉네임/목표 공부시간)
            User user100 = mock(User.class);
            when(user100.getNickname()).thenReturn("방장김철수");
            when(user100.getGoalStudyTime()).thenReturn(300);

            User user200 = mock(User.class);
            when(user200.getNickname()).thenReturn("참가자이영희");
            when(user200.getGoalStudyTime()).thenReturn(240);

            when(userRepository.findById(100L)).thenReturn(Optional.of(user100));
            when(userRepository.findById(200L)).thenReturn(Optional.of(user200));

            // 오늘 공부시간 계산은 내부에서 studyTimeRepository 호출 → 빈 리스트로 두면 0분 처리
            when(studyTimeRepository.findAllByUserIdAndCreatedAtBetween(anyLong(), any(), any()))
                    .thenReturn(List.of());

            // when
            ParticipantStatusResponse res = participantService.getParticipantStatus(roomId);

            // then
            assertThat(res).isNotNull();
            assertThat(res.getParticipants()).hasSize(2);

            ParticipantInfo p0 = res.getParticipants().get(0);
            ParticipantInfo p1 = res.getParticipants().get(1);

            // 방장 정보 체크(소유자 플래그, 닉네임, 토글 상태)
            assertThat(p0.getUserId()).isEqualTo(100L);
            assertThat(p0.getNickname()).isEqualTo("방장김철수");
            assertThat(p0.getIsOwner()).isTrue();
            assertThat(p0.getAudioEnabled()).isTrue();
            assertThat(p0.getVideoEnabled()).isTrue();
            assertThat(p0.getTargetStudyTime()).isEqualTo(300);
            assertThat(p0.getTodayStudyTime()).isEqualTo(0); // today=0 (레코드 없으면 0)

            // 일반 참가자
            assertThat(p1.getUserId()).isEqualTo(200L);
            assertThat(p1.getNickname()).isEqualTo("참가자이영희");
            assertThat(p1.getIsOwner()).isFalse();
            assertThat(p1.getAudioEnabled()).isFalse();
            assertThat(p1.getVideoEnabled()).isTrue();
            assertThat(p1.getTargetStudyTime()).isEqualTo(240);
            assertThat(p1.getTodayStudyTime()).isEqualTo(0);

            RoomInfo info = res.getRoomInfo();
            assertThat(info.getTotalParticipants()).isEqualTo(2);
            assertThat(info.getIsAllMuted()).isFalse();

            // verify (선택)
            verify(studyRoomRepository).findById(roomId);
            verify(participantRepository).findActiveParticipantsByRoomId(roomId);
            verify(userRepository, times(2)).findById(anyLong());
        }

        @Test
        @DisplayName("성공: 활성 참가자가 없으면 participants=[], totalParticipants=0")
        void 성공_빈방() {
            // given
            Long roomId = 10L;

            StudyRoom room = mock(StudyRoom.class);
            when(room.isAllMuted()).thenReturn(false);
            // 🔥 room.getUserId() 삭제 - 빈 방에서는 사용되지 않음

            when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

            // 🔥 중복 제거 - 하나만 남기기
            when(participantRepository.findActiveParticipantsByRoomId(roomId))
                    .thenReturn(Collections.emptyList());

            // when
            ParticipantStatusResponse res = participantService.getParticipantStatus(roomId);

            // then
            assertThat(res).isNotNull();
            assertThat(res.getParticipants()).isEmpty();
            assertThat(res.getRoomInfo().getTotalParticipants()).isEqualTo(0);
            assertThat(res.getRoomInfo().getIsAllMuted()).isFalse();
        }
        @Test
        @DisplayName("실패: 방이 존재하지 않으면 ParticipantException.StudyRoomNotFoundException 발생")
        void 실패_방없음() {
            // given
            Long roomId = 404L;
            when(studyRoomRepository.findById(roomId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> participantService.getParticipantStatus(roomId))
                    .isInstanceOf(ParticipantException.StudyRoomNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("보조 쿼리 메서드")
    class Helpers {

        @Test
        @DisplayName("getActiveParticipants(roomId): 리포지토리 위임")
        void getActiveParticipants() {
            Long roomId = 7L;
            when(participantRepository.findActiveParticipantsByRoomId(roomId))
                    .thenReturn(List.of(mock(Participant.class)));

            List<Participant> list = participantService.getActiveParticipants(roomId);
            assertThat(list).hasSize(1);
            verify(participantRepository).findActiveParticipantsByRoomId(roomId);
        }

        @Test
        @DisplayName("getActiveParticipantCount(roomId): 리포지토리 위임")
        void getActiveParticipantCount() {
            Long roomId = 7L;
            when(participantRepository.countActiveParticipantsByRoomId(roomId)).thenReturn(3L);

            long count = participantService.getActiveParticipantCount(roomId);
            assertThat(count).isEqualTo(3L);
            verify(participantRepository).countActiveParticipantsByRoomId(roomId);
        }
    }
}
