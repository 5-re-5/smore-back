package org.oreo.smore.domain.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.dto.TokenResponse;
import org.oreo.smore.domain.video.exception.OwnerNotJoinedException;
import org.oreo.smore.domain.video.exception.StudyRoomNotFoundException;
import org.oreo.smore.domain.video.exception.WrongPasswordException;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VideoCallControllerTest {

    @Mock
    private StudyRoomValidator studyRoomValidator;

    @Mock
    private LiveKitTokenService tokenService;

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @InjectMocks
    private VideoCallController videoCallController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private StudyRoom 테스트방;
    private JoinRoomRequest 입장요청;
    private TokenResponse 토큰응답;

    @BeforeEach
    void setUp() {
        // MockMvc 수동 설정
        mockMvc = MockMvcBuilders.standaloneSetup(videoCallController)
                .defaultRequest(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .build();
        objectMapper = new ObjectMapper();

        // 테스트용 스터디룸
        테스트방 = StudyRoom.builder()
                .userId(1L)  // 방장 ID = 1
                .title("테스트 스터디방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .password(null)
                .liveKitRoomId("study-room-123")  // 방장 이미 입장
                .build();

        // 입장 요청
        입장요청 = JoinRoomRequest.builder()
                .identity("테스트사용자")
                .canPublish(true)
                .canSubscribe(true)
                .build();

        // 토큰 응답
        토큰응답 = TokenResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token")
                .roomName("study-room-123")
                .identity("테스트사용자")
                .canPublish(true)
                .canSubscribe(true)
                .expiresAt(LocalDateTime.now().plusSeconds(3600))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 방장_첫_입장_API_성공() throws Exception {
        // given
        Long roomId = 123L;
        Long 방장ID = 1L;

        when(studyRoomValidator.validateRoomAccess(eq(roomId), any(JoinRoomRequest.class), eq(방장ID)))
                .thenReturn(테스트방);
        when(tokenService.generateToken(any())).thenReturn(토큰응답);

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", roomId)
                        .param("userId", 방장ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token"))
                .andExpect(jsonPath("$.roomName").value("study-room-123"))
                .andExpect(jsonPath("$.identity").value("테스트사용자"));
    }

    @Test
    void 참가자가_방장보다_먼저_입장_시도_403에러() throws Exception {
        // given
        Long roomId = 123L;
        Long 참가자ID = 2L;

        when(studyRoomValidator.validateRoomAccess(eq(roomId), any(JoinRoomRequest.class), eq(참가자ID)))
                .thenThrow(new OwnerNotJoinedException("방장이 아직 방에 입장하지 않았습니다. 방장이 먼저 입장한 후 참가하세요."));

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", roomId)
                        .param("userId", 참가자ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void 방장_입장_후_참가자_입장_성공() throws Exception {
        // given
        Long roomId = 123L;
        Long 참가자ID = 3L;

        when(studyRoomValidator.validateRoomAccess(eq(roomId), any(JoinRoomRequest.class), eq(참가자ID)))
                .thenReturn(테스트방);
        when(tokenService.generateToken(any())).thenReturn(토큰응답);

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", roomId)
                        .param("userId", 참가자ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.identity").value("테스트사용자"));
    }

    @Test
    void 존재하지_않는_방_입장_시도_404에러() throws Exception {
        // given
        Long 없는방ID = 999L;
        Long 사용자ID = 1L;

        when(studyRoomValidator.validateRoomAccess(eq(없는방ID), any(JoinRoomRequest.class), eq(사용자ID)))
                .thenThrow(new StudyRoomNotFoundException("방을 찾을 수 없습니다. roomId: " + 없는방ID));

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 없는방ID)
                        .param("userId", 사용자ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void 비밀번호_틀린_방_입장_시도_401에러() throws Exception {
        // given
        Long roomId = 123L;
        Long 사용자ID = 2L;

        when(studyRoomValidator.validateRoomAccess(eq(roomId), any(JoinRoomRequest.class), eq(사용자ID)))
                .thenThrow(new WrongPasswordException("비밀번호가 틀렸습니다."));

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", roomId)
                        .param("userId", 사용자ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰_재발급_API_성공() throws Exception {
        // given
        Long roomId = 123L;
        Long 사용자ID = 1L;

        when(studyRoomValidator.validateRoomAccess(eq(roomId), any(JoinRoomRequest.class), eq(사용자ID)))
                .thenReturn(테스트방);
        when(tokenService.regenerateToken(eq("study-room-123"), eq("테스트사용자")))
                .thenReturn(토큰응답);

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/rejoin", roomId)
                        .param("userId", 사용자ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.roomName").value("study-room-123"));
    }

    @Test
    void 잘못된_요청_데이터_400에러() throws Exception {
        // given - identity가 빈 문자열인 잘못된 요청
        JoinRoomRequest 잘못된요청 = JoinRoomRequest.builder()
                .identity("")  // 빈 문자열 (validation 오류)
                .canPublish(true)
                .build();

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 123L)
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(잘못된요청)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 한글_사용자명으로_입장_성공() throws Exception {
        // given
        Long roomId = 123L;
        Long 사용자ID = 1L;

        JoinRoomRequest 한글요청 = JoinRoomRequest.builder()
                .identity("김철수")
                .canPublish(true)
                .canSubscribe(true)
                .build();

        TokenResponse 한글토큰응답 = TokenResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.korean.token")
                .roomName("study-room-123")
                .identity("김철수")
                .canPublish(true)
                .canSubscribe(true)
                .expiresAt(LocalDateTime.now().plusSeconds(3600))
                .createdAt(LocalDateTime.now())
                .build();

        when(studyRoomValidator.validateRoomAccess(eq(roomId), any(JoinRoomRequest.class), eq(사용자ID)))
                .thenReturn(테스트방);
        when(tokenService.generateToken(any())).thenReturn(한글토큰응답);

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", roomId)
                        .param("userId", 사용자ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(한글요청)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.identity").value("김철수"));
    }

    @Test
    void userId_파라미터_누락시_400에러() throws Exception {
        // given - userId 파라미터 없이 요청

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 123L)
                        // .param("userId", "1")  // 의도적으로 누락
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(입장요청)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}