package org.oreo.smore.domain.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.ParticipantService;
import org.oreo.smore.domain.participant.dto.UpdatePersonalStatusRequest;
import org.oreo.smore.domain.participant.dto.UpdatePersonalStatusResponse;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
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
@DisplayName("VideoCallController - 개인 상태 변경 API 테스트")
class VideoCallControllerUpdateTest {

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
    private ObjectMapper objectMapper;
    private StudyRoom mockStudyRoom;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(videoCallController).build();
        objectMapper = new ObjectMapper();

        mockStudyRoom = StudyRoom.builder()
                .roomId(1L)
                .userId(100L)
                .title("테스트 방")
                .build();
    }

    // ==================== 메인 API 테스트 ====================

    @Test
    @DisplayName("개인 상태 변경 성공 - 본인 요청")
    void updateParticipantStatus_Success_SelfUpdate() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(false)
                .build();

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                100L, "김철수", true, false);

        when(participantService.updatePersonalMediaStatus(roomId, userId, true, false))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.updateParticipantStatus(roomId, userId, request, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getUserId()).isEqualTo(100L);
        assertThat(result.getBody().getNickname()).isEqualTo("김철수");
        assertThat(result.getBody().getAudioEnabled()).isTrue();
        assertThat(result.getBody().getVideoEnabled()).isFalse();
        assertThat(result.getBody().getMessage()).isEqualTo("개인 미디어 상태가 성공적으로 변경되었습니다");

        verify(participantService).updatePersonalMediaStatus(roomId, userId, true, false);
    }

    @Test
    @DisplayName("개인 상태 변경 성공 - 방장이 다른 참가자 상태 변경")
    void updateParticipantStatus_Success_OwnerUpdate() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long ownerUserId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(false)
                .videoEnabled(true)
                .build();

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                200L, "이영희", false, true);

        // 방장 권한 검증 통과

        when(participantService.updatePersonalMediaStatus(roomId, targetUserId, false, true))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.updateParticipantStatus(roomId, targetUserId, request, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getUserId()).isEqualTo(200L);
        assertThat(result.getBody().getNickname()).isEqualTo("이영희");
        assertThat(result.getBody().getAudioEnabled()).isFalse();
        assertThat(result.getBody().getVideoEnabled()).isTrue();

        verify(studyRoomValidator).validateOwnerPermission(roomId, ownerUserId);
        verify(participantService).updatePersonalMediaStatus(roomId, targetUserId, false, true);
    }

    @Test
    @DisplayName("개인 상태 변경 실패 - 권한 없음 (다른 사용자가 변경 시도)")
    void updateParticipantStatus_Forbidden_UnauthorizedUser() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long requestUserId = 300L;  // 방장도 아니고 본인도 아닌 사용자
        Authentication auth = new UsernamePasswordAuthenticationToken("300", null);

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(true)
                .build();

        // 방장 권한 검증 실패
        when(studyRoomValidator.validateOwnerPermission(roomId, requestUserId))
                .thenThrow(new RuntimeException("방장 권한이 없습니다"));

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.updateParticipantStatus(roomId, targetUserId, request, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNull();

        verify(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);
        verifyNoInteractions(participantService);
    }

    @Test
    @DisplayName("개인 상태 변경 실패 - 인증 없음")
    void updateParticipantStatus_Forbidden_NoAuthentication() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = null;  // 인증 없음

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(false)
                .build();

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.updateParticipantStatus(roomId, userId, request, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNull();

        verifyNoInteractions(studyRoomValidator, participantService);
    }

    @Test
    @DisplayName("개인 상태 변경 실패 - 참가자 없음")
    void updateParticipantStatus_BadRequest_ParticipantNotFound() {
        // Given
        Long roomId = 1L;
        Long userId = 999L;
        Authentication auth = new UsernamePasswordAuthenticationToken("999", null);

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(false)
                .build();

        when(participantService.updatePersonalMediaStatus(roomId, userId, true, false))
                .thenThrow(new ParticipantException.ParticipantNotFoundException("참가자를 찾을 수 없습니다"));

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.updateParticipantStatus(roomId, userId, request, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isNull();

        verify(participantService).updatePersonalMediaStatus(roomId, userId, true, false);
    }

    @Test
    @DisplayName("개인 상태 변경 실패 - 시스템 오류")
    void updateParticipantStatus_InternalServerError() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(false)
                .build();

        when(participantService.updatePersonalMediaStatus(roomId, userId, true, false))
                .thenThrow(new IllegalStateException("DB 연결 오류"));

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.updateParticipantStatus(roomId, userId, request, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNull();

        verify(participantService).updatePersonalMediaStatus(roomId, userId, true, false);
    }

    // ==================== 오디오 토글 API 테스트 ====================

    @Test
    @DisplayName("오디오 토글 성공 - 본인 요청")
    void toggleParticipantAudio_Success_SelfToggle() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                100L, "김철수", true, false);

        when(participantService.toggleAudioStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.toggleParticipantAudio(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getUserId()).isEqualTo(100L);
        assertThat(result.getBody().getAudioEnabled()).isTrue();

        verify(participantService).toggleAudioStatus(roomId, userId);
    }

    @Test
    @DisplayName("오디오 토글 성공 - 방장이 다른 참가자 토글")
    void toggleParticipantAudio_Success_OwnerToggle() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long ownerUserId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                200L, "이영희", false, true);

        // 방장 권한 검증 통과
        when(studyRoomValidator.validateOwnerPermission(roomId, ownerUserId))
                .thenReturn(mockStudyRoom);

        when(participantService.toggleAudioStatus(roomId, targetUserId))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.toggleParticipantAudio(roomId, targetUserId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getAudioEnabled()).isFalse();

        verify(studyRoomValidator).validateOwnerPermission(roomId, ownerUserId);
        verify(participantService).toggleAudioStatus(roomId, targetUserId);
    }

    @Test
    @DisplayName("오디오 토글 실패 - 권한 없음")
    void toggleParticipantAudio_Forbidden() {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long requestUserId = 300L;
        Authentication auth = new UsernamePasswordAuthenticationToken("300", null);

        // 방장 권한 검증 실패
        when(studyRoomValidator.validateOwnerPermission(roomId, requestUserId))
                .thenThrow(new RuntimeException("방장 권한이 없습니다"));

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.toggleParticipantAudio(roomId, targetUserId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNull();

        verify(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);
        verifyNoInteractions(participantService);
    }

    // ==================== 비디오 토글 API 테스트 ====================

    @Test
    @DisplayName("비디오 토글 성공 - 본인 요청")
    void toggleParticipantVideo_Success_SelfToggle() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                100L, "김철수", true, true);

        when(participantService.toggleVideoStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.toggleParticipantVideo(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getVideoEnabled()).isTrue();

        verify(participantService).toggleVideoStatus(roomId, userId);
    }

    @Test
    @DisplayName("비디오 토글 실패 - 인증 없음")
    void toggleParticipantVideo_Forbidden_NoAuth() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = null;

        // When
        ResponseEntity<UpdatePersonalStatusResponse> result =
                videoCallController.toggleParticipantVideo(roomId, userId, auth);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNull();

        verifyNoInteractions(studyRoomValidator, participantService);
    }

    // ==================== MockMvc 통합 테스트 ====================

    @Test
    @DisplayName("개인 상태 변경 - MockMvc 테스트 (성공)")
    void updateParticipantStatus_MockMvc_Success() throws Exception {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(false)
                .build();

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                100L, "김철수", true, false);

        when(participantService.updatePersonalMediaStatus(roomId, userId, true, false))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(patch("/v1/study-rooms/{roomId}/participants/{userId}", roomId, userId)
                        .principal(new UsernamePasswordAuthenticationToken("100", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.nickname").value("김철수"))
                .andExpect(jsonPath("$.audioEnabled").value(true))
                .andExpect(jsonPath("$.videoEnabled").value(false))
                .andExpect(jsonPath("$.message").value("개인 미디어 상태가 성공적으로 변경되었습니다"));

        verify(participantService).updatePersonalMediaStatus(roomId, userId, true, false);
    }

    @Test
    @DisplayName("오디오 토글 - MockMvc 테스트 (성공)")
    void toggleParticipantAudio_MockMvc_Success() throws Exception {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                100L, "김철수", true, false);

        when(participantService.toggleAudioStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(patch("/v1/study-rooms/{roomId}/participants/{userId}/audio/toggle", roomId, userId)
                        .principal(new UsernamePasswordAuthenticationToken("100", null))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.audioEnabled").value(true));

        verify(participantService).toggleAudioStatus(roomId, userId);
    }

    @Test
    @DisplayName("비디오 토글 - MockMvc 테스트 (성공)")
    void toggleParticipantVideo_MockMvc_Success() throws Exception {
        // Given
        Long roomId = 1L;
        Long userId = 100L;

        UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                100L, "김철수", false, true);

        when(participantService.toggleVideoStatus(roomId, userId))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(patch("/v1/study-rooms/{roomId}/participants/{userId}/video/toggle", roomId, userId)
                        .principal(new UsernamePasswordAuthenticationToken("100", null))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoEnabled").value(true));

        verify(participantService).toggleVideoStatus(roomId, userId);
    }

    @Test
    @DisplayName("개인 상태 변경 - MockMvc 테스트 (권한 없음)")
    void updateParticipantStatus_MockMvc_Forbidden() throws Exception {
        // Given
        Long roomId = 1L;
        Long targetUserId = 200L;
        Long requestUserId = 300L;

        UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                .audioEnabled(true)
                .videoEnabled(false)
                .build();

        when(studyRoomValidator.validateOwnerPermission(roomId, requestUserId))
                .thenThrow(new RuntimeException("방장 권한이 없습니다"));

        // When & Then
        mockMvc.perform(patch("/v1/study-rooms/{roomId}/participants/{userId}", roomId, targetUserId)
                        .principal(new UsernamePasswordAuthenticationToken("300", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(studyRoomValidator).validateOwnerPermission(roomId, requestUserId);
        verifyNoInteractions(participantService);
    }

    @Test
    @DisplayName("다양한 미디어 상태 조합 테스트")
    void updateParticipantStatus_VariousCombinations() {
        // Given
        Long roomId = 1L;
        Long userId = 100L;
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        // 테스트 케이스들
        Object[][] testCases = {
                {true, true, "둘 다 켜기"},
                {false, false, "둘 다 끄기"},
                {true, false, "마이크만 켜기"},
                {false, true, "카메라만 켜기"}
        };

        for (Object[] testCase : testCases) {
            Boolean audioEnabled = (Boolean) testCase[0];
            Boolean videoEnabled = (Boolean) testCase[1];
            String description = (String) testCase[2];

            // Given
            UpdatePersonalStatusRequest request = UpdatePersonalStatusRequest.builder()
                    .audioEnabled(audioEnabled)
                    .videoEnabled(videoEnabled)
                    .build();

            UpdatePersonalStatusResponse mockResponse = UpdatePersonalStatusResponse.success(
                    100L, "김철수", audioEnabled, videoEnabled);

            when(participantService.updatePersonalMediaStatus(roomId, userId, audioEnabled, videoEnabled))
                    .thenReturn(mockResponse);

            // When
            ResponseEntity<UpdatePersonalStatusResponse> result =
                    videoCallController.updateParticipantStatus(roomId, userId, request, auth);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getAudioEnabled()).isEqualTo(audioEnabled);
            assertThat(result.getBody().getVideoEnabled()).isEqualTo(videoEnabled);

            System.out.println("✅ " + description + " 테스트 통과");
        }
    }
}