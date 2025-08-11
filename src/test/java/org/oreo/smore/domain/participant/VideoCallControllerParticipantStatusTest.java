package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.participant.dto.ParticipantStatusResponse;
import org.oreo.smore.domain.participant.dto.RoomInfo;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.video.controller.VideoCallController;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VideoCallControllerParticipantStatusTest {

    // 컨트롤러 생성자에 들어가는 모든 의존성 mock
    @Mock StudyRoomValidator studyRoomValidator;
    @Mock LiveKitTokenService tokenService;
    @Mock StudyRoomRepository studyRoomRepository;
    @Mock UserIdentityService userIdentityService;
    @Mock ParticipantService participantService;
    @Mock StudyRoomService studyRoomService;

    @InjectMocks
    private VideoCallController videoCallController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Security 안 붙이고 순수 MVC만 테스트
        mockMvc = MockMvcBuilders.standaloneSetup(videoCallController).build();
    }

    @Test
    void 참가자_상태_조회_성공_200() throws Exception {
        long roomId = 123L;

        // 예시: DTO 실제 인스턴스 생성 (필드명/빌더명은 프로젝트에 맞춰 수정)
        RoomInfo roomInfo = RoomInfo.builder()
                .isAllMuted(false)
                .totalParticipants(0)  // 이 필드도 필요할 수 있음
                .build();

        ParticipantStatusResponse response = ParticipantStatusResponse.builder()
                .roomInfo(roomInfo)
                .participants(Collections.emptyList())
                .build();

        when(participantService.getParticipantStatus(roomId)).thenReturn(response);

        // 🔥 URL 수정: /status 제거
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants", roomId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void 참가자_상태_조회_런타임오류_400() throws Exception {
        long roomId = 123L;

        when(participantService.getParticipantStatus(roomId))
                .thenThrow(new RuntimeException("비즈니스 오류"));  // 🔥 RuntimeException으로 변경

        // 🔥 URL 수정: /status 제거
        mockMvc.perform(get("/v1/study-rooms/{roomId}/participants", roomId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}