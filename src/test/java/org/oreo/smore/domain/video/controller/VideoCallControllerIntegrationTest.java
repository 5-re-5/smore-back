package org.oreo.smore.domain.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.global.common.CloudStorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testUser", roles = {"USER"})
@ActiveProfiles("test")
@Transactional
class VideoCallControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    private StudyRoom 테스트방;
    private StudyRoom 비밀방;

    // 🔥 TestConfiguration으로 Mock Bean 생성
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary  // 실제 Bean 대신 이걸 사용
        public CloudStorageManager mockCloudStorageManager() {
            return Mockito.mock(CloudStorageManager.class);
        }
    }

    @BeforeEach
    void setUp() {
        // 실제 DB에 테스트 데이터 생성
        테스트방 = StudyRoom.builder()
                .userId(1L)
                .title("실제 통합 테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .password(null)
                .build();
        테스트방 = studyRoomRepository.save(테스트방);

        비밀방 = StudyRoom.builder()
                .userId(2L)
                .title("실제 비밀 테스트 방")
                .category(StudyRoomCategory.CERTIFICATION)
                .maxParticipants(4)
                .password("1234")
                .build();
        비밀방 = studyRoomRepository.save(비밀방);
    }

//    @Test
//    void 실제_LiveKit_서버_방장_입장_테스트() throws Exception {
//        // given
//        JoinRoomRequest 방장요청 = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .build();
//
//        // when & then
//        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", 테스트방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(방장요청)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.accessToken").isString())
//                .andExpect(jsonPath("$.roomName").value("study-room-" + 테스트방.getRoomId()))
//                .andExpect(jsonPath("$.identity").value("실제방장"))
//                .andExpect(jsonPath("$.canPublish").value(true))
//                .andExpect(jsonPath("$.canSubscribe").value(true))
//                .andExpect(jsonPath("$.expiresAt").exists())
//                .andExpect(jsonPath("$.createdAt").exists());
//
//        System.out.println("🎉 실제 LiveKit 토큰 발급 성공!");
//    }
//
//    @Test
//    void 실제_LiveKit_서버_참가자_입장_테스트() throws Exception {
//        // given - 먼저 방장이 입장해야 함
//        JoinRoomRequest 방장요청 = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .build();
//
//        // 방장 먼저 입장
//        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", 테스트방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(방장요청)))
//                .andExpect(status().isOk());
//
//        // 참가자 입장 요청
//        JoinRoomRequest 참가자요청 = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .build();
//
//        // when & then
//        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", "999")  // 다른 사용자 ID
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(참가자요청)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.identity").value("실제참가자"));
//
//        System.out.println("✅ 참가자 입장 및 토큰 발급 성공!");
//    }

    @Test
    void 방장_미입장_상태에서_참가자_입장_시도_실패() throws Exception {
        // given - 방장이 입장하지 않은 상태
        JoinRoomRequest 참가자요청 = JoinRoomRequest.builder()
                .canPublish(true)
                .canSubscribe(true)
                .build();

        // when & then
        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
                        .param("userId", "999")  // 방장이 아닌 사용자
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(참가자요청)))
                .andDo(print())
                .andExpect(status().isForbidden());

        System.out.println("🚫 방장 미입장으로 참가자 입장 차단 성공!");
    }

//    @Test
//    void 실제_토큰_길이_검증() throws Exception {
//        // given
//        JoinRoomRequest request = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .tokenExpirySeconds(7200)  // 2시간
//                .build();
//
//        // when & then
//        String response = mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", 테스트방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        // 실제 응답 출력
//        System.out.println("📋 실제 발급된 토큰 응답:");
//        System.out.println(response);
//        System.out.println("✅ 실제 JWT 토큰 발급 확인!");
//    }
//
//    @Test
//    void 실제_LiveKit_서버_비밀방_입장_테스트() throws Exception {
//        // given
//        JoinRoomRequest 방장요청 = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .password("1234")  // 올바른 비밀번호
//                .build();
//
//        // when & then
//        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 비밀방.getRoomId())
//                        .param("userId", 비밀방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(방장요청)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.identity").value("비밀방방장"));
//
//        System.out.println("🔐 비밀방 입장 및 토큰 발급 성공!");
//    }
//
//    @Test
//    void 실제_LiveKit_서버_토큰_재발급_테스트() throws Exception {
//        // given - 첫 번째 토큰 발급
//        JoinRoomRequest 첫번째요청 = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .build();
//
//        // 첫 번째 토큰 발급
//        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", 테스트방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(첫번째요청)))
//                .andExpect(status().isOk());
//
//        // 두 번째 토큰 발급 (재발급)
//        JoinRoomRequest 두번째요청 = JoinRoomRequest.builder()
//                .canPublish(false)  // 권한 변경
//                .canSubscribe(true)
//                .build();
//
//        // when & then
//        mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", 테스트방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(두번째요청)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.canPublish").value(false));
//
//        System.out.println("🔄 토큰 재발급 성공!");
//    }
//
//    @Test
//    void 실제_토큰_유효성_검증() throws Exception {
//        // given
//        JoinRoomRequest request = JoinRoomRequest.builder()
//                .canPublish(true)
//                .canSubscribe(true)
//                .tokenExpirySeconds(3600)
//                .build();
//
//        // when & then
//        String response = mockMvc.perform(post("/v1/study-rooms/{roomId}/join", 테스트방.getRoomId())
//                        .param("userId", 테스트방.getUserId().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        System.out.println("🔍 실제 토큰 유효성 검증:");
//        System.out.println(response);
//        System.out.println("✅ 토큰 형식 및 길이 검증 완료!");
//    }
}