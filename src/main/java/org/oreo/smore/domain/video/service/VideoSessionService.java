package org.oreo.smore.domain.video.service;

import io.openvidu.java.client.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.SessionCreateRequest;
import org.oreo.smore.domain.video.dto.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import static org.oreo.smore.global.exception.VideoExceptions.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class VideoSessionService {
    private final OpenVidu openVidu;
    private final StudyRoomRepository studyRoomRepository;

    @Transactional
    public SessionResponse createSession(@Valid SessionCreateRequest request) {
        // 어떤 roomId, 어떤 customSessionId 로 클라이언트 요청이 들어왔는지 기록
        log.info("[시작] 세션 생성 요청 - roomId={}, customSessionId={}",
                request.getRoomId(), request.getCustomSessionId());

        // 스터디룸 조회
        StudyRoom studyRoom = findActiveStudyRoom(request.getRoomId());
        log.debug("[검증] 활성 스터디룸 조회 완료 - roomId={}", studyRoom.getRoomId());

        // 세션 중복 확인
        validateSessionCreation(studyRoom);
        log.debug("[검증] 세션 중복 여부 확인 완료");

        try {
            // SessionProperties 빌더 준비
            SessionProperties.Builder builder = new SessionProperties.Builder();

            // customSessionId 가 주어졌으면 반영
            if (Objects.nonNull(request.getCustomSessionId()) &&
                    !request.getCustomSessionId().trim().isEmpty()) {
                builder.customSessionId(request.getCustomSessionId().trim());
                log.debug("[옵션] customSessionId 설정: {}", request.getCustomSessionId());
            }

            // 빌더에서 SessionProperties 생성
            SessionProperties properties = builder.build();

            // OpenVidu 서버에 세션 요청
            Session session = openVidu.createSession(properties);
            log.info("[성공] OpenVidu 세션 생성 완료 - sessionId={}", session.getSessionId());

            // StudyRoom 엔티티에 세션ID 할당
            studyRoom.assignOpenViduSession(session.getSessionId());
            log.debug("StudyRoom에 sessionId 할당 완료");

            // 응답용 DTO 변환
            SessionResponse response = SessionResponse.of(session, studyRoom);
            log.info("[완료] 세션 생성 응답 준비 - sessionId={}", response.getSessionId());

            return response;
        } catch (OpenViduJavaClientException e) {

            log.error("[실패] OpenVidu 클라이언트 오류 - {}", e.getMessage(), e);
            throw new VideoSessionException("클라이언트 오류: " + e.getMessage(), e);

        } catch (OpenViduHttpException e) {

            log.error("[실패] OpenVidu 서버 오류 - status={}, message={}",
                    e.getStatus(), e.getMessage(), e);
            throw new VideoSessionException(
                    String.format("서버 오류(%d): %s", e.getStatus(), e.getMessage()), e);
        }
    }

    @Transactional(readOnly = true)
    public StudyRoom findActiveStudyRoom(Long roomId) {
        return studyRoomRepository
                .findByRoomIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new StudyRoomNotFoundException(
                "활성 스터디룸을 찾을 수 없습니다. roomId=" + roomId));
    }

    private void validateSessionCreation(StudyRoom studyRoom) {
        if (Objects.nonNull(studyRoom.getOpenViduSessionId())) {
            throw new SessionAlreadyExistsException(
                    "이미 세션이 존재합니다. sessionId=" + studyRoom.getOpenViduSessionId());
        }
    }
}
