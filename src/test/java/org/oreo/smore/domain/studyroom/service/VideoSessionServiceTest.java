//package org.oreo.smore.domain.studyroom.service;
//import io.openvidu.java.client.OpenVidu;
//import io.openvidu.java.client.Session;
//import io.openvidu.java.client.SessionProperties;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.oreo.smore.domain.studyroom.StudyRoom;
//import org.oreo.smore.domain.studyroom.StudyRoomCategory;
//import org.oreo.smore.domain.studyroom.StudyRoomRepository;
//import org.oreo.smore.domain.video.dto.SessionCreateRequest;
//import org.oreo.smore.domain.video.dto.SessionResponse;
//import org.oreo.smore.domain.video.service.VideoSessionService;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.ArgumentMatchers.any;
//
//import static org.oreo.smore.global.exception.VideoExceptions.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("VideoSessionService 테스트")
//class VideoSessionServiceTest {
//    @Mock private OpenVidu openVidu;
//    @Mock private StudyRoomRepository studyRoomRepository;
//    @Mock private Session session;
//    // 실제 인스턴스로 생성하되 위 Mock 필드들을 생성자의 파라미터로 주입
//    @InjectMocks private VideoSessionService videoSessionService;
//
//    @Test
//    @DisplayName("세션 생성 성공")
//    void createSession_Success() throws Exception {
//        // given
//        Long roomId = 1L;
//        String sessionId = "ses_java_study";
//        // 요청 DTO 준비 (roomId + customSessionId)
//        SessionCreateRequest request = new SessionCreateRequest(roomId, "java_study");
//        // DB에서 반환될 StudyRoom 엔티티 -> 초기에는 sessionId가 null
//        StudyRoom studyRoom = new StudyRoom(1L, roomId,"Java 스터디", StudyRoomCategory.EMPLOYMENT);
//
//        given(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
//                .willReturn(Optional.of(studyRoom));
//        given(openVidu.createSession(any(SessionProperties.class)))
//                .willReturn(session);
//        given(session.getSessionId()).willReturn(sessionId);
//
//        // when
//        SessionResponse response = videoSessionService.createSession(request);
//
//        // then
//        assertThat(response.getSessionId()).isEqualTo(sessionId);
//        assertThat(response.getRoomId()).isEqualTo(roomId);
//        assertThat(studyRoom.getOpenViduSessionId()).isEqualTo(sessionId);
//    }
//
//    @Test
//    @DisplayName("세션 중복 예외")
//    void createSession_SessionAlreadyExists() {
//        // given
//        Long roomId = 1L;
//        String existingSessionId = "ses_existing";
//        SessionCreateRequest request = new SessionCreateRequest(roomId);
//
//        StudyRoom studyRoom = new StudyRoom(1L, roomId,"Java 스터디", StudyRoomCategory.EMPLOYMENT);
//        studyRoom.assignOpenViduSession(existingSessionId);
//
//        given(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
//                .willReturn(Optional.of(studyRoom));
//
//        // when & then
//        assertThatThrownBy(() -> videoSessionService.createSession(request))
//                .isInstanceOf(SessionAlreadyExistsException.class)
//                .hasMessageContaining("sessionId=" + existingSessionId);
//    }
//}
