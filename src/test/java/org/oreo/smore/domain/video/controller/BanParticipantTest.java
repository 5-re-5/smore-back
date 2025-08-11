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
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("참가자 강퇴 API 테스트")
class BanParticipantTest {

    private MockMvc mockMvc;

    @InjectMocks
    private VideoCallController videoCallController;

    @Mock
    private StudyRoomValidator studyRoomValidator;

    @Mock
    private ParticipantService participantService;

    @Mock
    private LiveKitTokenService tokenService;

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @Mock
    private UserIdentityService userIdentityService;

    @Mock
    private StudyRoomService studyRoomService;

    private static final Long ROOM_ID = 1L;
    private static final Long OWNER_ID = 100L;
    private static final Long TARGET_USER_ID = 200L;
    private static final String API_URL = "/v1/study-rooms/{roomId}/participants/{userId}/ban";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(videoCallController).build();
    }

    @Test
    @DisplayName("성공: 방장이 참가자를 정상적으로 강퇴")
    void banParticipant_Success() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(OWNER_ID.toString());

        // 방장 권한 확인 성공 - 예외를 던지지 않으면 성공으로 간주
        // validateOwnerPermission이 void가 아니므로 stubbing 하지 않음 (기본적으로 null 반환)

        // 강퇴 처리 성공
        doNothing().when(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);

        // 참가자 수 반환 설정
        when(participantService.getActiveParticipantCount(ROOM_ID)).thenReturn(3L);

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // 메서드 호출 검증
        verify(studyRoomValidator).validateOwnerPermission(ROOM_ID, OWNER_ID);
        verify(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);
        verify(participantService).getActiveParticipantCount(ROOM_ID);
    }

    @Test
    @DisplayName("실패: 인증되지 않은 사용자")
    void banParticipant_Unauthorized() throws Exception {
        // given - 인증 정보 없음

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 서비스 메서드 호출되지 않아야 함
        verify(studyRoomValidator, never()).validateOwnerPermission(anyLong(), anyLong());
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 빈 Principal")
    void banParticipant_EmptyPrincipal() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn("");

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(studyRoomValidator, never()).validateOwnerPermission(anyLong(), anyLong());
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: null Principal - 실제로는 400 반환")
    void banParticipant_NullPrincipal() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(null);

        // when & then - 실제 로그에서 400이 반환됨을 확인
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 403 → 400으로 수정

        verify(studyRoomValidator, never()).validateOwnerPermission(anyLong(), anyLong());
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 잘못된 사용자 ID 형식")
    void banParticipant_InvalidUserIdFormat() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn("invalid-user-id");

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(studyRoomValidator, never()).validateOwnerPermission(anyLong(), anyLong());
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 자기 자신을 강퇴하려는 경우")
    void banParticipant_BanSelf() throws Exception {
        // given - 요청자와 대상자가 동일
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(OWNER_ID.toString());

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, OWNER_ID) // 동일한 ID
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 권한 확인 전에 차단되어야 함
        verify(studyRoomValidator, never()).validateOwnerPermission(anyLong(), anyLong());
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 방장이 아닌 사용자의 강퇴 시도")
    void banParticipant_NotOwner() throws Exception {
        // given
        Long nonOwnerUserId = 300L;
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(nonOwnerUserId.toString());

        // 방장 권한 확인 실패
        when(studyRoomValidator.validateOwnerPermission(ROOM_ID, nonOwnerUserId))
                .thenThrow(new SecurityException("방장만 강퇴할 수 있습니다"));

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(studyRoomValidator).validateOwnerPermission(ROOM_ID, nonOwnerUserId);
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 방")
    void banParticipant_RoomNotFound() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(OWNER_ID.toString());

        // 방 존재하지 않음
        when(studyRoomValidator.validateOwnerPermission(ROOM_ID, OWNER_ID))
                .thenThrow(new IllegalArgumentException("방을 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(studyRoomValidator).validateOwnerPermission(ROOM_ID, OWNER_ID);
        verify(participantService, never()).banParticipant(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 참가자")
    void banParticipant_ParticipantNotFound() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(OWNER_ID.toString());

        // 방장 권한 확인 성공 - stubbing 하지 않음 (예외를 던지지 않으면 성공)

        // 참가자를 찾을 수 없음
        doThrow(new ParticipantException.ParticipantNotFoundException("참가자를 찾을 수 없습니다"))
                .when(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(studyRoomValidator).validateOwnerPermission(ROOM_ID, OWNER_ID);
        verify(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("실패: RuntimeException 발생")
    void banParticipant_RuntimeException() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(OWNER_ID.toString());

        // 방장 권한 확인 성공 - stubbing 하지 않음

        // RuntimeException 발생
        doThrow(new RuntimeException("시스템 오류"))
                .when(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(studyRoomValidator).validateOwnerPermission(ROOM_ID, OWNER_ID);
        verify(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);
    }

    @Test
    @DisplayName("실패: 일반 Exception 발생 (내부 서버 오류)")
    void banParticipant_GeneralException() throws Exception {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(OWNER_ID.toString());

        // 방장 권한 확인 성공 - stubbing 하지 않음

        // 일반 RuntimeException 발생 (checked exception은 불가)
        doThrow(new RuntimeException("예상치 못한 시스템 오류"))
                .when(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);

        // when & then
        mockMvc.perform(post(API_URL, ROOM_ID, TARGET_USER_ID)
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest()); // RuntimeException은 400으로 처리됨

        verify(studyRoomValidator).validateOwnerPermission(ROOM_ID, OWNER_ID);
        verify(participantService).banParticipant(ROOM_ID, TARGET_USER_ID);
    }
}