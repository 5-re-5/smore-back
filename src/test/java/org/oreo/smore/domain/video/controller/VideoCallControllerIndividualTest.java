package org.oreo.smore.domain.video.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.ParticipantService;
import org.oreo.smore.domain.participant.dto.IndividualParticipantResponse;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoCallController - 개인 참가자 조회 API 테스트")
class VideoCallControllerIndividualTest {
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
    @DisplayName("개인 참가자 조회 성공 - 본인 조회")
    void getIndividualParticipant_Success_SelfInquiry() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        IndividualParticipantResponse mockResponse = IndividualParticipantResponse.builder()
                .userId(100L)
                .nickname("김철수")
                .isOwner(true)
                .audioEnabled(true)
                .videoEnabled(false)
                .todayStudyTime(180)
                .targetStudyTime(300)
                .isInRoom(true)
                .roomName("스터디 룸 A")
                .totalParticipants(3)
                .build();

        when(participantService.getIndividualParticipantStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getUserId()).isEqualTo(100L);
        assertThat(result.getBody().getNickname()).isEqualTo("김철수");
        assertThat(result.getBody().getIsOwner()).isTrue();
        assertThat(result.getBody().getAudioEnabled()).isTrue();
        assertThat(result.getBody().getVideoEnabled()).isFalse();
        assertThat(result.getBody().getTodayStudyTime()).isEqualTo(180);
        assertThat(result.getBody().getTargetStudyTime()).isEqualTo(300);
        assertThat(result.getBody().getRoomName()).isEqualTo("스터디 룸 A");
        assertThat(result.getBody().getTotalParticipants()).isEqualTo(3);

        verify(participantService).getIndividualParticipantStatus(roomId, userId);
    }

    @Test
    @DisplayName("개인 참가자 조회 성공 - 방장이 다른 참가자 조회")
    void getIndividualParticipant_Success_OwnerInquiry() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long ownerUserId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        // validateOwnerPermission이 반환할 더미 방
        StudyRoom ownerRoom = StudyRoom.builder()
                .roomId(roomId)
                .userId(ownerUserId)               // 방장 = 100
                .title("스터디 룸 A")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        IndividualParticipantResponse mockResponse = IndividualParticipantResponse.builder()
                .userId(200L)
                .nickname("이영희")
                .isOwner(false)
                .audioEnabled(false)
                .videoEnabled(true)
                .todayStudyTime(120)
                .targetStudyTime(240)
                .isInRoom(true)
                .roomName("스터디 룸 A")
                .totalParticipants(3)
                .build();

        // 방장 권한 검증 통과: StudyRoom을 리턴해야 함
        when(studyRoomValidator.validateOwnerPermission(eq(roomId), eq(ownerUserId)))
                .thenReturn(ownerRoom);

        when(participantService.getIndividualParticipantStatus(roomId, targetUserId))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, targetUserId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getUserId()).isEqualTo(200L);
        assertThat(result.getBody().getNickname()).isEqualTo("이영희");
        assertThat(result.getBody().getIsOwner()).isFalse();
        assertThat(result.getBody().getAudioEnabled()).isFalse();
        assertThat(result.getBody().getVideoEnabled()).isTrue();

        verify(studyRoomValidator).validateOwnerPermission(roomId, ownerUserId);
        verify(participantService).getIndividualParticipantStatus(roomId, targetUserId);
    }


    @Test
    @DisplayName("개인 참가자 조회 실패 - 권한 없음 (다른 사용자가 조회 시도)")
    void getIndividualParticipant_Forbidden_UnauthorizedUser() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long requestUserId = 300L;  // 방장도 아니고 본인도 아닌 사용자
        Authentication auth = new UsernamePasswordAuthenticationToken("300", null);

        // 방장 권한 검증 실패
        doThrow(new RuntimeException("방장 권한이 없습니다"))
                .when(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, targetUserId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNull();

        verify(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);
        verifyNoInteractions(participantService);
    }

    @Test
    @DisplayName("개인 참가자 조회 실패 - 인증 없음")
    void getIndividualParticipant_Forbidden_NoAuthentication() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Authentication auth = null;  // 인증 없음

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, targetUserId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNull();

        verifyNoInteractions(studyRoomValidator, participantService);
    }

    @Test
    @DisplayName("개인 참가자 조회 실패 - 참가자 없음")
    void getIndividualParticipant_NotFound() {
        // Given
        Long roomId = 1L;
        Long userId = 999L;
        Authentication auth = new UsernamePasswordAuthenticationToken("999", null);

        when(participantService.getIndividualParticipantStatus(roomId, userId))
                .thenThrow(new ParticipantException.ParticipantNotFoundException("참가자를 찾을 수 없습니다"));

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isNull();

        verify(participantService).getIndividualParticipantStatus(roomId, userId);
    }

    @Test
    @DisplayName("개인 참가자 조회 실패 - 존재하지 않는 방")
    void getIndividualParticipant_RoomNotFound() {
        // Given
        Long roomId = 999L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        when(participantService.getIndividualParticipantStatus(roomId, userId))
                .thenThrow(new RuntimeException("방을 찾을 수 없습니다"));

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isNull();

        verify(participantService).getIndividualParticipantStatus(roomId, userId);
    }

    @Test
    @DisplayName("개인 참가자 조회 실패 - 시스템 오류")
    void getIndividualParticipant_SystemError() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        when(participantService.getIndividualParticipantStatus(roomId, userId))
                .thenThrow(new IllegalStateException("DB 연결 오류"));

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isNull();

        verify(participantService).getIndividualParticipantStatus(roomId, userId);
    }

    @Test
    @DisplayName("개인 참가자 조회 - MockMvc 테스트 (성공)")
    void getIndividualParticipant_MockMvc_Success() throws Exception {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        IndividualParticipantResponse mockResponse = IndividualParticipantResponse.builder()
                .userId(100L)
                .nickname("김철수")
                .isOwner(true)
                .audioEnabled(true)
                .videoEnabled(true)
                .todayStudyTime(120)
                .targetStudyTime(240)
                .isInRoom(true)
                .roomName("테스트 방")
                .totalParticipants(2)
                .build();

        when(participantService.getIndividualParticipantStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants/{userId}", roomId, userId)
                        .principal(new UsernamePasswordAuthenticationToken("100", null))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.nickname").value("김철수"))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.audioEnabled").value(true))
                .andExpect(jsonPath("$.videoEnabled").value(true))
                .andExpect(jsonPath("$.todayStudyTime").value(120))
                .andExpect(jsonPath("$.targetStudyTime").value(240))
                .andExpect(jsonPath("$.isInRoom").value(true))
                .andExpect(jsonPath("$.roomName").value("테스트 방"))
                .andExpect(jsonPath("$.totalParticipants").value(2));

        verify(participantService).getIndividualParticipantStatus(roomId, userId);
    }

    @Test
    @DisplayName("개인 참가자 조회 - MockMvc 테스트 (권한 없음)")
    void getIndividualParticipant_MockMvc_Forbidden() throws Exception {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long requestUserId = 300L;

        // 방장 권한 검증 실패
        doThrow(new RuntimeException("방장 권한이 없습니다"))
                .when(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);

        // When & Then
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants/{userId}", roomId, targetUserId)
                        .principal(new UsernamePasswordAuthenticationToken("300", null))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);
        verifyNoInteractions(participantService);
    }

    @Test
    @DisplayName("개인 참가자 조회 - 다양한 미디어 상태")
    void getIndividualParticipant_VariousMediaStates() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        IndividualParticipantResponse mockResponse = IndividualParticipantResponse.builder()
                .userId(100L)
                .nickname("테스트사용자")
                .isOwner(false)
                .audioEnabled(false)  // 마이크 꺼짐
                .videoEnabled(false)  // 카메라 꺼짐
                .todayStudyTime(0)    // 공부시간 0분
                .targetStudyTime(600) // 목표시간 10시간
                .isInRoom(true)
                .roomName("조용한 방")
                .totalParticipants(1)
                .build();

        when(participantService.getIndividualParticipantStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<IndividualParticipantResponse> result =
                videoCallController.getIndividualParticipant(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getAudioEnabled()).isFalse();
        assertThat(result.getBody().getVideoEnabled()).isFalse();
        assertThat(result.getBody().getTodayStudyTime()).isEqualTo(0);
        assertThat(result.getBody().getIsOwner()).isFalse();

        verify(participantService).getIndividualParticipantStatus(roomId, userId);
    }
}
