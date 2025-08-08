package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.dto.ParticipantInfo;
import org.oreo.smore.domain.participant.dto.ParticipantStatusResponse;
import org.oreo.smore.domain.participant.dto.RoomInfo;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.video.controller.VideoCallController;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoCallController - 참가자 상태 조회 API 테스트")
class VideoCallControllerStatusTest {

    @Mock
    private ParticipantService participantService;

    @Mock
    private StudyRoomValidator studyRoomValidator;

    @Mock
    private LiveKitTokenService tokenService;

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @Mock
    private UserIdentityService userIdentityService;

    @Mock
    private StudyRoomService studyRoomService;

    @InjectMocks
    private VideoCallController videoCallController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(videoCallController).build();
    }

    @Test
    @DisplayName("참가자 상태 조회 API 성공 - 방장 + 일반 참가자")
    void getParticipantStatus_Success() {
        // Given
        Long roomId = 1L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        ParticipantStatusResponse mockResponse = ParticipantStatusResponse.builder()
                .participants(List.of(
                        ParticipantInfo.builder()
                                .userId(100L)
                                .nickname("김철수")
                                .isOwner(true)
                                .audioEnabled(true)
                                .videoEnabled(false)
                                .todayStudyTime(180)
                                .targetStudyTime(300)
                                .build(),
                        ParticipantInfo.builder()
                                .userId(200L)
                                .nickname("이영희")
                                .isOwner(false)
                                .audioEnabled(false)
                                .videoEnabled(true)
                                .todayStudyTime(120)
                                .targetStudyTime(240)
                                .build()
                ))
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(false)
                        .totalParticipants(2)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(mockResponse);

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParticipants()).hasSize(2);
        assertThat(result.getBody().getRoomInfo().getTotalParticipants()).isEqualTo(2);
        assertThat(result.getBody().getRoomInfo().getIsAllMuted()).isFalse();

        // 방장 정보 검증
        var owner = result.getBody().getParticipants().stream()
                .filter(ParticipantInfo::getIsOwner)
                .findFirst().orElse(null);
        assertThat(owner).isNotNull();
        assertThat(owner.getUserId()).isEqualTo(100L);
        assertThat(owner.getNickname()).isEqualTo("김철수");
        assertThat(owner.getAudioEnabled()).isTrue();
        assertThat(owner.getVideoEnabled()).isFalse();

        // 일반 참가자 정보 검증
        var participant = result.getBody().getParticipants().stream()
                .filter(p -> !p.getIsOwner())
                .findFirst().orElse(null);
        assertThat(participant).isNotNull();
        assertThat(participant.getUserId()).isEqualTo(200L);
        assertThat(participant.getNickname()).isEqualTo("이영희");
        assertThat(participant.getAudioEnabled()).isFalse();
        assertThat(participant.getVideoEnabled()).isTrue();

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - 빈 방")
    void getParticipantStatus_EmptyRoom() {
        // Given
        Long roomId = 1L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        ParticipantStatusResponse emptyResponse = ParticipantStatusResponse.builder()
                .participants(List.of())
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(false)
                        .totalParticipants(0)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(emptyResponse);

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParticipants()).isEmpty();
        assertThat(result.getBody().getRoomInfo().getTotalParticipants()).isEqualTo(0);
        assertThat(result.getBody().getRoomInfo().getIsAllMuted()).isFalse();

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - 전체 음소거 활성화")
    void getParticipantStatus_AllMuted() {
        // Given
        Long roomId = 1L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        ParticipantStatusResponse response = ParticipantStatusResponse.builder()
                .participants(List.of(
                        ParticipantInfo.builder()
                                .userId(100L)
                                .nickname("방장")
                                .isOwner(true)
                                .audioEnabled(true)
                                .videoEnabled(true)
                                .todayStudyTime(90)
                                .targetStudyTime(240)
                                .build()
                ))
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(true)  // 전체 음소거 활성화
                        .totalParticipants(1)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(response);

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getRoomInfo().getIsAllMuted()).isTrue();
        assertThat(result.getBody().getRoomInfo().getTotalParticipants()).isEqualTo(1);

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API 실패 - 서비스 예외 (RuntimeException)")
    void getParticipantStatus_ServiceException() {
        // Given
        Long roomId = 999L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        when(participantService.getParticipantStatus(roomId))
                .thenThrow(new RuntimeException("방을 찾을 수 없습니다"));

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isNull();

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API 실패 - 시스템 오류 (Exception)")
    void getParticipantStatus_SystemError() {
        // Given
        Long roomId = 1L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        when(participantService.getParticipantStatus(roomId))
                .thenThrow(new IllegalStateException("DB 연결 오류"));

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNull();

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - 인증 없음 (공개 접근)")
    void getParticipantStatus_NoAuthentication() {
        // Given
        Long roomId = 1L;
        Authentication auth = null;  // 인증 없음

        ParticipantStatusResponse mockResponse = ParticipantStatusResponse.builder()
                .participants(List.of(
                        ParticipantInfo.builder()
                                .userId(100L)
                                .nickname("방장")
                                .isOwner(true)
                                .audioEnabled(true)
                                .videoEnabled(false)
                                .todayStudyTime(0)
                                .targetStudyTime(300)
                                .build()
                ))
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(false)
                        .totalParticipants(1)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(mockResponse);

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then - 인증 없어도 조회 가능 (공개 API)
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParticipants()).hasSize(1);

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - MockMvc 테스트 (성공)")
    void getParticipantStatus_MockMvc_Success() throws Exception {
        // Given
        Long roomId = 1L;

        ParticipantStatusResponse mockResponse = ParticipantStatusResponse.builder()
                .participants(List.of(
                        ParticipantInfo.builder()
                                .userId(100L)
                                .nickname("김철수")
                                .isOwner(true)
                                .audioEnabled(true)
                                .videoEnabled(true)
                                .todayStudyTime(120)
                                .targetStudyTime(240)
                                .build()
                ))
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(false)
                        .totalParticipants(1)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants/status", roomId)
                        .principal(new UsernamePasswordAuthenticationToken("100", null))
                        .accept(MediaType.APPLICATION_JSON))  // ✅ Accept 헤더 추가
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))  // ✅ MediaType 상수 사용
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].userId").value(100))
                .andExpect(jsonPath("$.participants[0].nickname").value("김철수"))
                .andExpect(jsonPath("$.participants[0].isOwner").value(true))
                .andExpect(jsonPath("$.participants[0].audioEnabled").value(true))
                .andExpect(jsonPath("$.participants[0].videoEnabled").value(true))
                .andExpect(jsonPath("$.participants[0].todayStudyTime").value(120))
                .andExpect(jsonPath("$.participants[0].targetStudyTime").value(240))
                .andExpect(jsonPath("$.roomInfo.totalParticipants").value(1))
                .andExpect(jsonPath("$.roomInfo.isAllMuted").value(false));

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - MockMvc 테스트 (RuntimeException)")
    void getParticipantStatus_MockMvc_RuntimeException() throws Exception {
        // Given
        Long roomId = 999L;

        when(participantService.getParticipantStatus(roomId))
                .thenThrow(new RuntimeException("방을 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants/status", roomId)
                        .principal(new UsernamePasswordAuthenticationToken("100", null))
                        .accept(MediaType.APPLICATION_JSON))  // ✅ Accept 헤더 추가
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - MockMvc 테스트 (시스템 오류)")
    void getParticipantStatus_MockMvc_SystemError() throws Exception {
        // Given
        Long roomId = 1L;

        when(participantService.getParticipantStatus(roomId))
                .thenThrow(new IllegalStateException("DB 연결 실패"));

        // When & Then
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants/status", roomId)
                        .accept(MediaType.APPLICATION_JSON))  // ✅ Accept 헤더 추가
                .andDo(print())
                .andExpect(status().isInternalServerError());  // ✅ 500 기대 (컨트롤러 수정 후)

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - 미디어 상태 다양한 조합 테스트")
    void getParticipantStatus_VariousMediaStates() {
        // Given
        Long roomId = 1L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        ParticipantStatusResponse mockResponse = ParticipantStatusResponse.builder()
                .participants(List.of(
                        // 마이크 O, 카메라 O
                        ParticipantInfo.builder()
                                .userId(100L).nickname("참가자1").isOwner(true)
                                .audioEnabled(true).videoEnabled(true)
                                .todayStudyTime(60).targetStudyTime(300)
                                .build(),
                        // 마이크 X, 카메라 O
                        ParticipantInfo.builder()
                                .userId(200L).nickname("참가자2").isOwner(false)
                                .audioEnabled(false).videoEnabled(true)
                                .todayStudyTime(120).targetStudyTime(240)
                                .build(),
                        // 마이크 O, 카메라 X
                        ParticipantInfo.builder()
                                .userId(300L).nickname("참가자3").isOwner(false)
                                .audioEnabled(true).videoEnabled(false)
                                .todayStudyTime(90).targetStudyTime(180)
                                .build(),
                        // 마이크 X, 카메라 X
                        ParticipantInfo.builder()
                                .userId(400L).nickname("참가자4").isOwner(false)
                                .audioEnabled(false).videoEnabled(false)
                                .todayStudyTime(30).targetStudyTime(120)
                                .build()
                ))
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(false)
                        .totalParticipants(4)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(mockResponse);

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getParticipants()).hasSize(4);

        List<ParticipantInfo> participants = result.getBody().getParticipants();

        // 각 참가자의 미디어 상태 확인
        assertThat(participants.get(0).getAudioEnabled()).isTrue();   // 참가자1: 마이크 O
        assertThat(participants.get(0).getVideoEnabled()).isTrue();   // 참가자1: 카메라 O

        assertThat(participants.get(1).getAudioEnabled()).isFalse();  // 참가자2: 마이크 X
        assertThat(participants.get(1).getVideoEnabled()).isTrue();   // 참가자2: 카메라 O

        assertThat(participants.get(2).getAudioEnabled()).isTrue();   // 참가자3: 마이크 O
        assertThat(participants.get(2).getVideoEnabled()).isFalse();  // 참가자3: 카메라 X

        assertThat(participants.get(3).getAudioEnabled()).isFalse();  // 참가자4: 마이크 X
        assertThat(participants.get(3).getVideoEnabled()).isFalse();  // 참가자4: 카메라 X

        verify(participantService).getParticipantStatus(roomId);
    }

    @Test
    @DisplayName("참가자 상태 조회 API - 공부시간 0분인 경우")
    void getParticipantStatus_ZeroStudyTime() {
        // Given
        Long roomId = 1L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        ParticipantStatusResponse mockResponse = ParticipantStatusResponse.builder()
                .participants(List.of(
                        ParticipantInfo.builder()
                                .userId(100L)
                                .nickname("신규참가자")
                                .isOwner(true)
                                .audioEnabled(true)
                                .videoEnabled(false)
                                .todayStudyTime(0)    // 오늘 공부시간 0분
                                .targetStudyTime(300) // 목표시간 5시간
                                .build()
                ))
                .roomInfo(RoomInfo.builder()
                        .isAllMuted(false)
                        .totalParticipants(1)
                        .build())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(mockResponse);

        // When
        ResponseEntity<ParticipantStatusResponse> result =
                videoCallController.getParticipantStatus(roomId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        ParticipantInfo participant = result.getBody().getParticipants().get(0);
        assertThat(participant.getTodayStudyTime()).isEqualTo(0);
        assertThat(participant.getTargetStudyTime()).isEqualTo(300);

        verify(participantService).getParticipantStatus(roomId);
    }
}