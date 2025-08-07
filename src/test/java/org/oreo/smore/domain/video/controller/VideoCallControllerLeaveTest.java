package org.oreo.smore.domain.video.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.ParticipantService;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoCallController - 방 퇴장 테스트")
class VideoCallControllerLeaveTest {

    @Mock
    private ParticipantService participantService;

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @Mock
    private StudyRoomService studyRoomService;

    @Mock
    private UserIdentityService userIdentityService;

    @InjectMocks
    private VideoCallController videoCallController;

    private MockMvc mockMvc;

    private Long roomId;
    private Long ownerId;      // 방장 ID
    private Long participantId; // 일반 참가자 ID
    private StudyRoom mockStudyRoom;

    @BeforeEach
    void setUp() {
        // MockMvc 설정
        mockMvc = MockMvcBuilders.standaloneSetup(videoCallController)
                .build();

        roomId = 1L;
        ownerId = 100L;        // 방장
        participantId = 200L;  // 일반 참가자

        // Mock StudyRoom 설정 (방장 = 100L)
        mockStudyRoom = StudyRoom.builder()
                .roomId(roomId)
                .userId(ownerId)  // 방장 설정
                .title("테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        System.out.println("🔧 테스트 준비 완료");
        System.out.println("   - 방ID: " + roomId);
        System.out.println("   - 방장ID: " + ownerId);
        System.out.println("   - 일반 참가자ID: " + participantId);
    }

    @Test
    @DisplayName("일반 참가자 퇴장 성공 - 방은 유지됨")
    void leaveRoom_ParticipantLeave_Success() throws Exception {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken("200", null);

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        // mockStudyRoom.userId = 100L (방장), 요청 userId = 200L (일반 참가자) → 방장 아님
        when(participantService.getActiveParticipantCount(roomId)).thenReturn(2L);

        // When & Then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", "200")
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify
        verify(studyRoomRepository).findById(roomId); // 방장 확인용
        verify(participantService).leaveRoom(roomId, 200L); // 개별 퇴장 호출
        verify(participantService).getActiveParticipantCount(roomId); // 남은 참가자 수 확인
        verify(studyRoomService, never()).deleteStudyRoomByOwnerLeave(any(), any()); // 방 삭제 안됨

        System.out.println("✅ 일반 참가자 퇴장 - 방 유지됨");
    }

    @Test
    @DisplayName("방장 퇴장 성공 - 방 삭제됨 (무조건!)")
    void leaveRoom_OwnerLeave_RoomDeleted() throws Exception {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));
        // mockStudyRoom.userId = 100L (방장), 요청 userId = 100L (방장) → 일치!

        // When & Then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", "100")
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify
        verify(studyRoomRepository).findById(roomId); // 방장 확인용
        verify(studyRoomService).deleteStudyRoomByOwnerLeave(roomId, 100L); // 방 삭제 호출
        verify(participantService, never()).leaveRoom(any(), any()); // 개별 퇴장 안됨

        System.out.println("✅ 방장 퇴장 → 방 삭제 (무조건!)");
    }

    @Test
    @DisplayName("방장 확인 로직 테스트")
    void leaveRoom_OwnerCheck() throws Exception {
        // Given - 방장 정보가 일치하는 경우
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        StudyRoom ownerRoom = StudyRoom.builder()
                .roomId(roomId)
                .userId(100L)  // 방장ID
                .title("방장 테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(ownerRoom));

        // When
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", "100") // 방장 본인
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isOk());

        // Then
        verify(studyRoomRepository).findById(roomId); // 방장 확인을 위해 조회
        verify(studyRoomService).deleteStudyRoomByOwnerLeave(roomId, 100L); // 방장이므로 방 삭제

        System.out.println("✅ 방장 확인 로직 - userId == studyRoom.userId → 방 삭제");
    }

    @Test
    @DisplayName("일반 참가자 vs 방장 구분 테스트")
    void leaveRoom_ParticipantVsOwner_Distinction() throws Exception {
        // Given
        StudyRoom room = StudyRoom.builder()
                .roomId(roomId)
                .userId(100L)  // 방장ID = 100
                .title("구분 테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(participantService.getActiveParticipantCount(roomId)).thenReturn(3L);

        // Test 1: 일반 참가자 (200L) 퇴장
        Authentication participantAuth = new UsernamePasswordAuthenticationToken("200", null);

        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", "200")
                        .principal(participantAuth))
                .andDo(print())
                .andExpect(status().isOk());

        verify(participantService).leaveRoom(roomId, 200L); // 개별 퇴장
        verify(studyRoomService, never()).deleteStudyRoomByOwnerLeave(any(), any());

        // Mock 초기화
        reset(participantService, studyRoomService, studyRoomRepository);
        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // Test 2: 방장 (100L) 퇴장
        Authentication ownerAuth = new UsernamePasswordAuthenticationToken("100", null);

        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", "100")
                        .principal(ownerAuth))
                .andDo(print())
                .andExpect(status().isOk());

        verify(studyRoomService).deleteStudyRoomByOwnerLeave(roomId, 100L); // 방 삭제
        verify(participantService, never()).leaveRoom(any(), any());

        System.out.println("✅ 참가자 vs 방장 구분 테스트 완료");
        System.out.println("   - 일반 참가자(200) → 개별 퇴장");
        System.out.println("   - 방장(100) → 방 삭제");
    }

    @Test
    @DisplayName("방장 퇴장으로 다른 참가자들 영향 테스트")
    void leaveRoom_OwnerLeave_AffectOtherParticipants() throws Exception {
        // Given - 방장 + 다른 참가자 2명 상황
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));

        // When
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", "100")
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isOk());

        // Then
        verify(studyRoomService).deleteStudyRoomByOwnerLeave(roomId, 100L);

        System.out.println("✅ 방장 퇴장으로 다른 참가자 영향 테스트 완료");
        System.out.println("   - 방장 퇴장 → 다른 참가자들도 강제 퇴장됨");
    }

    // ====================================================================
    // 🚨 예외 상황 테스트들
    // ====================================================================

    @Test
    @DisplayName("방 퇴장 실패 - 인증 실패 (다른 사용자 ID)")
    void leaveRoom_Fail_AuthenticationMismatch() throws Exception {
        // Given - 인증 사용자와 요청 사용자가 다름
        Authentication auth = new UsernamePasswordAuthenticationToken("999", null);

        // When & Then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", participantId.toString()) // userId=200, but 인증은 999
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isForbidden());

        // Verify - 인증 실패로 서비스 메서드 호출 안됨
        verify(studyRoomRepository, never()).findById(any());
        verify(participantService, never()).leaveRoom(any(), any());

        System.out.println("✅ 인증 실패 테스트 완료");
        System.out.println("   - 인증 사용자(999) != 요청 사용자(200) → Forbidden");
        System.out.println("   - 비즈니스 로직 실행 안됨");
    }

    @Test
    @DisplayName("방 퇴장 실패 - 인증 정보 없음")
    void leaveRoom_Fail_NoAuthentication() throws Exception {
        // When & Then - 인증 정보 없이 요청
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", participantId.toString()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // Verify
        verify(studyRoomRepository, never()).findById(any());
        verify(participantService, never()).leaveRoom(any(), any());

        System.out.println("✅ 인증 정보 없음 테스트 완료");
        System.out.println("   - Authentication 없음 → Unauthorized");
    }

    @Test
    @DisplayName("방 퇴장 실패 - 방이 존재하지 않음")
    void leaveRoom_Fail_RoomNotFound() throws Exception {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken("100", null);
        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.empty()); // 방 없음

        // When & Then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", ownerId.toString())
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 방장 권한 확인 실패로 400 에러

        // Verify
        verify(studyRoomRepository).findById(roomId);
        verify(participantService, never()).leaveRoom(any(), any());
        verify(studyRoomService, never()).deleteStudyRoomByOwnerLeave(any(), any());

        System.out.println("✅ 방 존재하지 않음 테스트 완료");
        System.out.println("   - studyRoomRepository.findById() → Optional.empty()");
        System.out.println("   - 방장 권한 확인 실패 → BadRequest");
    }

    @Test
    @DisplayName("방 퇴장 실패 - ParticipantService 오류")
    void leaveRoom_Fail_ServiceError() throws Exception {
        // Given - 서비스에서 예외 발생
        Authentication auth = new UsernamePasswordAuthenticationToken("200", null);

        when(studyRoomRepository.findById(roomId)).thenReturn(Optional.of(mockStudyRoom));

        // leaveRoom에서 예외 발생
        doThrow(new RuntimeException("DB 연결 오류"))
                .when(participantService).leaveRoom(roomId, participantId);

        // When & Then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", roomId)
                        .param("userId", participantId.toString())
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify - 예외 발생시에도 필요한 검증은 수행됨
        verify(studyRoomRepository).findById(roomId);
        verify(participantService).leaveRoom(roomId, participantId); // 여기서 예외 발생

        System.out.println("✅ ParticipantService 오류 테스트 완료");
        System.out.println("   - participantService.leaveRoom()에서 예외 발생");
        System.out.println("   - BadRequest 응답");
    }

    @Test
    @DisplayName("경계값 테스트 - 잘못된 파라미터")
    void leaveRoom_Fail_InvalidParameters() throws Exception {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken("200", null);

        // When & Then - roomId가 0 (잘못된 값)
        mockMvc.perform(post("/v1/study-rooms/{roomId}/leave", 0L)
                        .param("userId", participantId.toString())
                        .principal(auth))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify - 잘못된 파라미터로 서비스 호출 안됨
        verify(studyRoomRepository, never()).findById(any());
        verify(participantService, never()).leaveRoom(any(), any());

        System.out.println("✅ 잘못된 파라미터 테스트 완료");
        System.out.println("   - roomId = 0 → BadRequest");
        System.out.println("   - 서비스 호출 안됨");
    }
}
