package org.oreo.smore.domain.video.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.participant.Participant;
import org.oreo.smore.domain.participant.ParticipantService;
import org.oreo.smore.domain.participant.dto.ParticipantStatusResponse;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.dto.TokenRequest;
import org.oreo.smore.domain.video.dto.TokenResponse;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/study-rooms")
@RequiredArgsConstructor
public class VideoCallController {

    private final StudyRoomValidator studyRoomValidator;
    private final LiveKitTokenService tokenService;
    private final StudyRoomRepository studyRoomRepository;
    private final UserIdentityService userIdentityService;
    private final ParticipantService participantService;
    private final StudyRoomService studyRoomService;

    // 스터디룸 입장 토큰 발급
    @PostMapping("/{roomId}/join")
    public ResponseEntity<TokenResponse> joinRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @Valid @RequestBody JoinRoomRequest request,
            Authentication authentication) {

        try {
            String principal = authentication.getPrincipal().toString();
            if (!principal.equals(userId.toString())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Authentication validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // User 테이블에서 nickname 가져오기
        String userNickname = userIdentityService.generateIdentityForUser(userId);

        log.info("스터디룸 입장 요청 - 방 ID: {}, 사용자ID: {}, 닉네임: {}", roomId, userId, userNickname);

        // 참가자 등록
        try {
            // 참가자를 DB에 일단 먼저 등록
            Participant participant = participantService.joinRoom(roomId,  userId);
            log.info("✅ 참가자 DB 등록 완료 - 참가자ID: {}, 방ID: {}, 사용자ID: {}",
                    participant.getParticipantId(), roomId, userId);
        } catch (Exception e) {
            log.error("❌ 참가자 등록 실패 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        // 방 입장 검증
        StudyRoom studyRoom = studyRoomValidator.validateRoomAccess(roomId, request, userId);
        // 방 정보 로깅
        studyRoomValidator.logRoomInfo(studyRoom);
        // LiveKit 방ID
        String liveKitRoomName = ensureLiveKitRoom(studyRoom);
        if (liveKitRoomName == null || liveKitRoomName.trim().isEmpty()) {
            log.error("❌ LiveKit 방 ID가 없습니다 - 방ID: {}", roomId);
            throw new IllegalStateException("LiveKit 방 정보가 올바르지 않습니다.");
        }

        // LiveKit 토큰 생성 요청
        TokenRequest tokenRequest = TokenRequest.builder()
                .roomName(liveKitRoomName)
                .identity(userNickname)
                .canPublish(request.getCanPublish())
                .canSubscribe(request.getCanSubscribe())
                .tokenExpirySeconds(request.getTokenExpirySeconds())
                .build();

        TokenResponse tokenResponse = tokenService.generateToken(tokenRequest);

        log.info("✅ 스터디룸 입장 성공 - 방ID: {}, 사용자: [{}], 방장여부: [{}]",
                roomId, userNickname, studyRoomValidator.isRoomOwner(studyRoom, userId));


        return ResponseEntity.ok(tokenResponse);
    }

    // 네트워크 끊김 등 재입장할 경우
    @PostMapping("/{roomId}/rejoin")
    public ResponseEntity<TokenResponse> rejoinRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @Valid @RequestBody JoinRoomRequest request,
            Authentication authentication
    ) {

        try {
            String principal = authentication.getPrincipal().toString();
            if (!principal.equals(userId.toString())) {
                log.warn("User ID mismatch in rejoin - Principal: {}, Requested: {}", principal, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Authentication validation failed in rejoin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String userNickname = userIdentityService.generateIdentityForUser(userId);

        log.info("스터디룸 토큰 재발급 요청 - 방ID: {}, 사용자ID: {}, 닉네임: [{}]",
                roomId, userId, userNickname);

        // 현재 참가중인지 확인
        boolean isInRoom = participantService.isUserInRoom(roomId, userId);
        if (!isInRoom) {
            log.warn("❌ 재입장 실패: 참가하지 않은 사용자 - 방ID: {}, 사용자ID: {}", roomId, userId);
            return ResponseEntity.badRequest().build();
        }

        StudyRoom studyRoom = studyRoomValidator.validateRoomAccess(roomId, request, userId);

        String liveKitRoomName = studyRoom.hasLiveKitRoom()
                ? studyRoom.getLiveKitRoomId()
                : studyRoom.generateLiveKitRoomId();

        // 토큰 재발급
        TokenResponse tokenResponse = tokenService.regenerateToken(
                liveKitRoomName,
                userNickname
        );
        log.info("✅ 스터디룸 토큰 재발급 성공 - DB방ID: {}, LiveKit방: [{}], 닉네임: [{}]",
                roomId, liveKitRoomName, userNickname);

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<TokenResponse> leaveRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            Authentication authentication) {
        try {
            String principal = authentication.getPrincipal().toString();
            if (!principal.equals(userId.toString())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("인증 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("개별 참가자 퇴장 요청 - 방ID: {}, 사용자ID: {}", roomId, userId);

        try {
            if (roomId <= 0) {
                log.error("❌ 잘못된 방 ID - roomId: {}", roomId);
                return ResponseEntity.badRequest().build();
            }

            // 방 정보 조회해서 방장인지 확인
            StudyRoom studyRoom = studyRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomId));

            // 방장 여부 확인
            boolean isOwner = studyRoom.getUserId().equals(userId);

            if (isOwner) {
                // 방장 퇴장 -> 방 삭제
                studyRoomService.deleteStudyRoomByOwnerLeave(roomId, userId);
                log.warn("✅ 방장 퇴장으로 방 삭제 완료 - 방ID: {}, 방장ID: {}", roomId, userId);
            } else {
                // 일반 참가자 퇴장
                participantService.leaveRoom(roomId, userId);

                // 남은 참가자 수 확인 (테스트에서 기대하는 동작)
                long remainingCount = participantService.getActiveParticipantCount(roomId);
                log.info("✅ 개별 참가자 퇴장 완료 - 방ID: {}, 사용자ID: {}, 남은 참가자: {}명",
                        roomId, userId, remainingCount);
            }

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.error("❌ 참가자 퇴장 실패 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ParticipantException e) {
            log.error("❌ 참가자 퇴장 실패 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            // 🔥 RuntimeException도 BadRequest로 처리 (테스트 요구사항)
            log.error("❌ 참가자 퇴장 실패 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ 참가자 퇴장 중 시스템 오류 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 방장 나가기 -> 방 삭제
    @DeleteMapping("/{roomId}")
    public ResponseEntity<TokenResponse> deleteStudyRoom(
            @PathVariable Long roomId,
            @RequestParam Long ownerId,
            Authentication authentication) {

        try {
            String principal = authentication.getPrincipal().toString();
            if (!principal.equals(ownerId.toString())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("인증 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.warn("방 삭제 요청 - 방ID: {}, 방장ID: {}", roomId, ownerId);

        try {
            studyRoomService.deleteStudyRoom(roomId, ownerId);

            log.warn("방 삭제 완료 - 방ID: {}, 방장ID: {}", roomId, ownerId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ 방 삭제 실패 - 방ID: {}, 방장ID: {}, 오류: {}",
                    roomId, ownerId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }


    // 참가자 상태 조회
    @GetMapping("/{roomId}/participants/status")
    public ResponseEntity<ParticipantStatusResponse> getParticipantStatus(
            @PathVariable Long roomId,
            Authentication authentication) {

        try {
            // 인증 확인 (선택적 - 방 참가자만 조회 가능하게 할지 결정 필요)
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            log.info("참가자 상태 조회 요청 - 방ID: {}, 요청자: {}", roomId, principal);

            // 참가자 상태 정보 조회
            ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

            log.info("✅ 참가자 상태 조회 성공 - 방ID: {}, 참가자 수: {}명, 전체음소거: {}",
                    roomId, response.getParticipants().size(), response.getRoomInfo().getIsAllMuted());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ 참가자 상태 조회 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("❌ 참가자 상태 조회 중 시스템 오류 - 방ID: {}, 오류: {}",
                    roomId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
