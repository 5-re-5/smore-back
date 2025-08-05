package org.oreo.smore.domain.video.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.dto.TokenRequest;
import org.oreo.smore.domain.video.dto.TokenResponse;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/study-rooms")
@RequiredArgsConstructor
public class VideoCallController {

    private final StudyRoomValidator studyRoomValidator;
    private final LiveKitTokenService tokenService;
    private final StudyRoomRepository studyRoomRepository;

    // 스터디룸 입장 토큰 발급
    @PostMapping("/{roomId}/join")
    public ResponseEntity<TokenResponse> joinRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @Valid @RequestBody JoinRoomRequest request) {
        log.info("스터디룸 입장 요청 - 방ID: {}, 사용자: [{}]", roomId, request.getIdentity());

        // 방 입장 검증
        StudyRoom studyRoom = studyRoomValidator.validateRoomAccess(roomId, request);
        // 방 정보 로깅
        studyRoomValidator.logRoomInfo(studyRoom);
        // LiveKit 방ID
        String liveKitRoomName = ensureLiveKitRoom(studyRoom);

        // LiveKit 토큰 생성 요청
        TokenRequest tokenRequest = TokenRequest.builder()
                .roomName(liveKitRoomName)
                .identity(request.getIdentity())
                .canPublish(request.getCanPublish())
                .canSubscribe(request.getCanSubscribe())
                .tokenExpirySeconds(request.getTokenExpirySeconds())
                .build();

        TokenResponse tokenResponse = tokenService.generateToken(tokenRequest);

        log.info("✅ 스터디룸 입장 성공 - 방ID: {}, 사용자: [{}], 방장여부: [{}]",
                roomId, request.getIdentity(), studyRoomValidator.isRoomOwner(studyRoom, userId));


        return ResponseEntity.ok(tokenResponse);
    }

    // 네트워크 끊김 등 재입장할 경우
    @PostMapping("/{roomId}/rejoin")
    public ResponseEntity<TokenResponse> rejoinRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody JoinRoomRequest request
    ) {
        log.info("스터디룸 토큰 재발급 요청 - 방ID: {}, 사용자: [{}]", roomId, request.getIdentity());

        StudyRoom studyRoom = studyRoomValidator.validateRoomAccess(roomId, request);

        String liveKitRoomName = studyRoom.hasLiveKitRoom()
                ? studyRoom.getLiveKitRoomId()
                : studyRoom.generateLiveKitRoomId();

        // 토큰 재발급
        TokenResponse tokenResponse = tokenService.regenerateToken(
                liveKitRoomName,
                request.getIdentity()
        );
        log.info("✅ 스터디룸 토큰 재발급 성공 - DB방ID: {}, LiveKit방: [{}], 사용자: [{}]",
                roomId, liveKitRoomName, request.getIdentity());

        return ResponseEntity.ok(tokenResponse);
    }

    private String ensureLiveKitRoom(StudyRoom studyRoom) {
        // DB에 아직 LiveKit roomId 가 없으면 생성 후 저장
        // 있으면 그대로 리턴

        if (studyRoom.hasLiveKitRoom()) {
            // 기존 LiveKit 방 ID 사용
            log.debug("기존 LiveKit 방 사용 - DB방ID: {}, LiveKit방: [{}]",
                    studyRoom.getRoomId(), studyRoom.getLiveKitRoomId());
            return studyRoom.getLiveKitRoomId();
        }

        // 새 LiveKit 방 ID 생성 및 DB 저장
        String newLiveKitRoomId = studyRoom.generateLiveKitRoomId(); // "study-room-123"
        studyRoom.setLiveKitRoomId(newLiveKitRoomId);
        studyRoomRepository.save(studyRoom);

        log.info("새 LiveKit 방 생성 및 DB 저장 - DB방ID: {}, LiveKit방: [{}]",
                studyRoom.getRoomId(), newLiveKitRoomId);

        return newLiveKitRoomId;
    }
}
