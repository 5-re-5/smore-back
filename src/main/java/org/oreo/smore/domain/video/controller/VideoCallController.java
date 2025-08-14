package org.oreo.smore.domain.video.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.participant.Participant;
import org.oreo.smore.domain.participant.ParticipantService;
import org.oreo.smore.domain.participant.dto.*;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.dto.TokenRequest;
import org.oreo.smore.domain.video.dto.TokenResponse;
import org.oreo.smore.domain.video.exception.StudyRoomNotFoundException;
import org.oreo.smore.domain.video.service.LiveKitTokenService;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.oreo.smore.domain.video.validator.StudyRoomValidator;
import org.oreo.smore.global.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // 인증 검증
        validateAuthentication(authentication, userId);

        // User 테이블에서 nickname 가져오기
        String userNickname = userIdentityService.generateIdentityForUser(userId);

        log.info("스터디룸 입장 요청 - 방 ID: {}, 사용자ID: {}, 닉네임: {}", roomId, userId, userNickname);

        // 참가자 등록
        try {

            // 1. 방 존재 여부 확인 (404)
            StudyRoom studyRoom = studyRoomRepository.findById(roomId)
                    .orElseThrow(() -> new RoomNotFoundException(roomId));

            // 2. 방 정원 초과 확인 (409)
            long currentParticipants = participantService.getActiveParticipantCount(roomId);
            if (currentParticipants >= studyRoom.getMaxParticipants()) {
                throw new RoomCapacityExceededException(roomId, (int) currentParticipants, studyRoom.getMaxParticipants());
            }

            // 3. 비밀번호 검증 (403)
            try {
                studyRoom = studyRoomValidator.validateRoomAccess(roomId, request, userId);

            } catch (Exception e) {
                log.error("🔐 비밀번호 검증 실패 - 방ID: {}, 사용자ID: {}, 예외: {}, 메시지: {}",
                        roomId, userId, e.getClass().getSimpleName(), e.getMessage());

                // 예외 발생 시 방이 여전히 존재하는지 확인
                boolean roomStillExists = studyRoomRepository.existsById(roomId);
                if (!roomStillExists) {
                    log.error("❌ 검증 중 방이 삭제됨 - 방ID: {}, 사용자ID: {}", roomId, userId);
                    throw new RoomNotFoundException(roomId); // 404로 처리
                }

                // 방이 존재하면 비밀번호 오류로 처리
                log.error("🔐 비밀번호 오류로 판단 - 방ID: {}, 사용자ID: {}", roomId, userId);
                throw new IncorrectPasswordException(roomId); // 403으로 처리
            }

            // 참가자를 DB에 일단 먼저 등록
            Participant participant = participantService.joinRoom(roomId, userId);
            log.info("✅ 참가자 DB 등록 완료 - 참가자ID: {}, 방ID: {}, 사용자ID: {}",
                    participant.getParticipantId(), roomId, userId);

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

        } catch (RoomNotFoundException e) {
            log.error("❌ 방을 찾을 수 없음 - 방ID: {}, 사용자ID: {}", roomId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (RoomCapacityExceededException e) {
            log.error("❌ 방 정원 초과 - 방ID: {}, 현재 참가자: {}, 최대 참가자: {}",
                    roomId, e.getCurrentCount(), e.getMaxCapacity());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (IncorrectPasswordException e) {
            log.error("❌ 비밀번호 오류 - 방ID: {}, 사용자ID: {}", roomId, userId);
            // 401 대신 403 사용 (프론트 요구사항)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalStateException e) {
            log.error("❌ 시스템 상태 오류 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (ParticipantException e) {
            log.error("❌ 참가자 처리 오류 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();

        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage(), e);

            boolean roomStillExists = studyRoomRepository.existsById(roomId);
            if (!roomStillExists) {
                log.error("❌ 처리 중 방이 삭제됨 - 방ID: {}, 사용자ID: {}", roomId, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{roomId}/rejoin")
    public ResponseEntity<TokenResponse> rejoinRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @Valid @RequestBody JoinRoomRequest request,
            Authentication authentication) {

        validateAuthentication(authentication, userId);
        String userNickname = userIdentityService.generateIdentityForUser(userId);
        log.info("🔄 스터디룸 토큰 재발급 요청 - 방ID: {}, 사용자ID: {}, 닉네임: [{}]", roomId, userId, userNickname);

        try {
            // 1. 방 존재 여부 확인
            StudyRoom studyRoom = studyRoomRepository.findById(roomId)
                    .orElseThrow(() -> new RoomNotFoundException(roomId));

            boolean isOwner = studyRoom.getUserId().equals(userId);
            log.info(" REJOIN - 방장 여부: {} (방장ID: {}, 요청자ID: {})", isOwner, studyRoom.getUserId(), userId);

            // 2.  방장인 경우 특별 처리 (권한 검증 없이 무조건 성공)
            if (isOwner) {

                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
                // TODO : 새로 고침 추가 하기
//                log.info("👑 REJOIN - 방장 재입장 처리 시작 - 방ID: {}, 방장ID: {}", roomId, userId);
//
//                // 방장이 참가자 목록에 없다면 자동으로 추가 (데이터 정합성 보장)
//                boolean isInRoom = participantService.isUserInRoom(roomId, userId);
//                if (!isInRoom) {
//                    try {
//                        log.info("REJOIN - 방장을 참가자 목록에 재등록 - 방ID: {}, 방장ID: {}", roomId, userId);
//                        participantService.joinRoom(roomId, userId);
//                        log.info("REJOIN - 방장 참가자 재등록 완료 - 방ID: {}, 방장ID: {}", roomId, userId);
//                    } catch (Exception e) {
//                        log.warn("⚠REJOIN - 방장 참가자 재등록 실패하지만 계속 진행 - 방ID: {}, 방장ID: {}, 오류: {}",
//                                roomId, userId, e.getMessage());
//                    }
//                }
//
//                studyRoomValidator.logRoomInfo(studyRoom);
//
//                // LiveKit 방ID 확보
//                String liveKitRoomName = ensureLiveKitRoom(studyRoom);
//                if (liveKitRoomName == null || liveKitRoomName.trim().isEmpty()) {
//                    log.error("❌ LiveKit 방 ID가 없습니다 - 방ID: {}", roomId);
//                    throw new IllegalStateException("LiveKit 방 정보가 올바르지 않습니다.");
//                }
//
//                // 방장은 권한 검증 없이 바로 토큰 발급
//                TokenRequest tokenRequest = TokenRequest.builder()
//                        .roomName(liveKitRoomName)
//                        .identity(userNickname)
//                        .canPublish(request.getCanPublish())
//                        .canSubscribe(request.getCanSubscribe())
//                        .tokenExpirySeconds(request.getTokenExpirySeconds())
//                        .build();
//
//                TokenResponse tokenResponse = tokenService.generateToken(tokenRequest);
//                log.info("✅ 방장 재입장 성공 - DB방ID: {}, LiveKit방: [{}], 방장: [{}]", roomId, liveKitRoomName, userNickname);
//
//                return ResponseEntity.ok(tokenResponse);
            }

            // 3. 일반 참가자 처리 (기존 로직 유지)
            log.info("REJOIN - 일반 참가자 재입장 처리 - 방ID: {}, 사용자ID: {}", roomId, userId);

            boolean isInRoom = participantService.isUserInRoom(roomId, userId);
            log.info("REJOIN - 참가 여부: {} - 방ID: {}, 사용자ID: {}", isInRoom, roomId, userId);

            if (!isInRoom) {
                // 정원 확인
                long currentParticipants = participantService.getActiveParticipantCount(roomId);
                if (currentParticipants >= studyRoom.getMaxParticipants()) {
                    throw new RoomCapacityExceededException(roomId, (int) currentParticipants, studyRoom.getMaxParticipants());
                }

                // 재참가 처리
                try {
                    participantService.joinRoom(roomId, userId);
                    log.info("✅ REJOIN - 일반 참가자 재참가 완료 - 방ID: {}, 사용자ID: {}", roomId, userId);
                } catch (Exception e) {
                    log.error("❌ REJOIN - 재참가 실패 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
                    throw e;
                }
            }

            // 일반 참가자 권한 검증
            StudyRoom validatedRoom = studyRoomValidator.validateRejoinAccess(roomId, userId);
            String liveKitRoomName = validatedRoom.hasLiveKitRoom()
                    ? validatedRoom.getLiveKitRoomId()
                    : validatedRoom.generateLiveKitRoomId();

            // LiveKit 토큰 생성 요청
            TokenRequest tokenRequest = TokenRequest.builder()
                    .roomName(liveKitRoomName)
                    .identity(userNickname)
                    .canPublish(request.getCanPublish())
                    .canSubscribe(request.getCanSubscribe())
                    .tokenExpirySeconds(request.getTokenExpirySeconds())
                    .build();

            TokenResponse tokenResponse = tokenService.generateToken(tokenRequest);
            log.info("✅ 일반 참가자 재입장 성공 - DB방ID: {}, LiveKit방: [{}], 닉네임: [{}]", roomId, liveKitRoomName, userNickname);

            return ResponseEntity.ok(tokenResponse);

        } catch (RoomNotFoundException e) {
            log.error("❌ 재입장 실패: 방을 찾을 수 없음 - 방ID: {}, 사용자ID: {}", roomId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (RoomCapacityExceededException e) {
            log.error("❌ 재입장 실패: 방 정원 초과 - 방ID: {}, 현재: {}, 최대: {}", roomId, e.getCurrentCount(), e.getMaxCapacity());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (SecurityException e) {
            log.error("❌ 재입장 실패: 접근 권한 없음 - 방ID: {}, 사용자ID: {}", roomId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            log.error("❌ 재입장 실패: 시스템 상태 오류 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("❌ 재입장 실패: 예상치 못한 오류 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<TokenResponse> leaveRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            Authentication authentication) {

        // 인증 검증
        validateAuthentication(authentication, userId);

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

                // 남은 참가자 수 확인
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

        // 인증 검증
        validateAuthentication(authentication, ownerId);

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


    // 참가자 상태 전체 조회
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ParticipantStatusResponse> getParticipantStatus(
            @PathVariable Long roomId,
            Authentication authentication) {

        try {
            // 인증 확인
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            log.info("참가자 상태 조회 요청 - 방ID: {}, 요청자: {}", roomId, principal);

            // 참가자 상태 정보 조회
            ParticipantStatusResponse response = participantService.getParticipantStatus(roomId);

            log.info("✅ 참가자 상태 조회 성공 - 방ID: {}, 참가자 수: {}명, 전체음소거: {}",
                    roomId, response.getParticipants().size(), response.getRoomInfo().getIsAllMuted());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {  // ✅ IllegalStateException을 먼저 처리
            log.error("❌ 전체 참가자 조회 중 시스템 오류 - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (RuntimeException e) {
            log.error("❌ 전체 참가자 조회 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("❌ 전체 참가자 조회 중 시스템 오류 - 방ID: {}, 오류: {}",
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

    // 개인 참가자 상태 조회
    @GetMapping("/{roomId}/participants/{userId}")
    public ResponseEntity<IndividualParticipantResponse> getIndividualParticipant(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            // 인증 확인
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            log.info("개인 참가자 조회 요청 - 방ID: {}, 대상사용자ID: {}, 요청자: {}", roomId, userId, principal);

            // 본인 확인 또는 방장 권한 확인
            validateIndividualAccessPermission(roomId, userId, principal);

            // 개인 참가자 상태 조회
            IndividualParticipantResponse response = participantService.getIndividualParticipantStatus(roomId, userId);

            log.info("✅ 개인 참가자 조회 성공 - 방ID: {}, 사용자: [{}], 방장여부: {}",
                    roomId, response.getNickname(), response.getIsOwner());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | ParticipantException e) {
            log.error("❌ 개인 참가자 조회 실패 (잘못된 요청) - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (SecurityException e) {
            log.error("❌ 개인 참가자 조회 권한 없음 - 방ID: {}, 사용자ID: {}, 요청자: {}, 오류: {}",
                    roomId, userId, authentication != null ? authentication.getPrincipal() : null, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (RuntimeException e) {
            log.error("❌ 개인 참가자 조회 실패 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("❌ 개인 참가자 조회 중 시스템 오류 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 개인 조회 권한 검증
    private void validateIndividualAccessPermission(Long roomId, Long targetUserId, String principal) {

        // 인증되지 않은 사용자는 차단
        if (principal == null || principal.trim().isEmpty()) {
            log.warn("❌ 인증되지 않은 개인 참가자 조회 시도 - 방ID: {}, 대상사용자ID: {}", roomId, targetUserId);
            throw new SecurityException("인증이 필요합니다");
        }

        Long requestUserId;
        try {
            requestUserId = Long.parseLong(principal);
        } catch (NumberFormatException e) {
            log.error("❌ 잘못된 사용자 ID 형식 - principal: {}", principal);
            throw new SecurityException("잘못된 인증 정보입니다");
        }

        // 본인 조회인 경우 허용
        if (requestUserId.equals(targetUserId)) {
            log.debug("✅ 본인 조회 권한 확인 완료 - 사용자ID: {}", requestUserId);
            return;
        }

        // 방장 권한 확인
        try {
            studyRoomValidator.validateOwnerPermission(roomId, requestUserId);
            log.info("✅ 방장 권한으로 다른 참가자 조회 허용 - 방장ID: {}, 대상사용자ID: {}", requestUserId, targetUserId);

        } catch (Exception e) {
            log.warn("❌ 개인 참가자 조회 권한 없음 - 요청자ID: {}, 대상사용자ID: {}, 오류: {}",
                    requestUserId, targetUserId, e.getMessage());
            throw new SecurityException("본인 또는 방장만 조회할 수 있습니다");
        }
    }

    @PatchMapping("/{roomId}/participants/{userId}/debug")
    public ResponseEntity<String> debugUpdateParticipantStatus(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @RequestBody String rawBody,
            HttpServletRequest request,
            Authentication authentication) {

        log.info("🔍 DEBUG - 받은 원본 JSON: {}", rawBody);
        log.info("🔍 DEBUG - Content-Type: {}", request.getContentType());
        log.info("🔍 DEBUG - Method: {}", request.getMethod());
        log.info("🔍 DEBUG - URI: {}", request.getRequestURI());

        try {
            // ObjectMapper로 직접 파싱해보기
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(rawBody);

            log.info("🔍 DEBUG - 파싱된 JSON 구조: {}", jsonNode.toPrettyString());

            // 각 필드 확인
            JsonNode audioNode = jsonNode.get("audioEnabled");
            JsonNode videoNode = jsonNode.get("videoEnabled");
            JsonNode dataNode = jsonNode.get("data");

            log.info("🔍 DEBUG - audioEnabled 필드: {} (타입: {})",
                    audioNode, audioNode != null ? audioNode.getNodeType() : "null");
            log.info("🔍 DEBUG - videoEnabled 필드: {} (타입: {})",
                    videoNode, videoNode != null ? videoNode.getNodeType() : "null");
            log.info("🔍 DEBUG - data 필드: {} (타입: {})",
                    dataNode, dataNode != null ? dataNode.getNodeType() : "null");

            // data 필드가 있으면 그 안의 내용도 확인
            if (dataNode != null) {
                JsonNode innerAudio = dataNode.get("audioEnabled");
                JsonNode innerVideo = dataNode.get("videoEnabled");
                log.info("🔍 DEBUG - data.audioEnabled: {} (타입: {})",
                        innerAudio, innerAudio != null ? innerAudio.getNodeType() : "null");
                log.info("🔍 DEBUG - data.videoEnabled: {} (타입: {})",
                        innerVideo, innerVideo != null ? innerVideo.getNodeType() : "null");
            }

            return ResponseEntity.ok("디버깅 완료 - 로그 확인");

        } catch (Exception e) {
            log.error("🔍 DEBUG - JSON 파싱 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("JSON 파싱 실패: " + e.getMessage());
        }
    }


    // 개인 참가자 미디어 상태 변경
    @PatchMapping("/{roomId}/participants/{userId}")
    public ResponseEntity<UpdatePersonalStatusResponse> updateParticipantStatus(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePersonalStatusRequest request,
            Authentication authentication) {

        try {
            // 인증 확인
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            log.info("개인 참가자 상태 변경 요청 - 방ID: {}, 대상사용자ID: {}, 요청자: {}, 변경내용: {}",
                    roomId, userId, principal, request);

            // 본인 확인 또는 방장 권한 확인
            validateUpdatePermission(roomId, userId, principal, request);

            // 개인 미디어 상태 변경
            UpdatePersonalStatusResponse response = participantService.updatePersonalMediaStatus(
                    roomId, userId, request.getAudioEnabled(), request.getVideoEnabled());

            log.info("✅ 개인 참가자 상태 변경 성공 - 방ID: {}, 사용자: [{}], 오디오: {}, 비디오: {}",
                    roomId, response.getNickname(), response.getAudioEnabled(), response.getVideoEnabled());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | ParticipantException e) {
            log.error("❌ 개인 참가자 상태 변경 실패 (잘못된 요청) - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (SecurityException e) {
            log.error("❌ 개인 참가자 상태 변경 권한 없음 - 방ID: {}, 사용자ID: {}, 요청자: {}, 오류: {}",
                    roomId, userId, authentication != null ? authentication.getPrincipal() : null, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalStateException e) {
            log.error("❌ 개인 참가자 상태 변경 중 시스템 오류 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (RuntimeException e) {
            log.error("❌ 개인 참가자 상태 변경 실패 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("❌ 개인 참가자 상태 변경 중 시스템 오류 - 방ID: {}, 사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 개인 상태 변경 권한 검증
    private void validateUpdatePermission(Long roomId, Long targetUserId, String principal,
                                          UpdatePersonalStatusRequest request) {

        // 인증되지 않은 사용자는 차단
        if (principal == null || principal.trim().isEmpty()) {
            log.warn("❌ 인증되지 않은 개인 참가자 상태 변경 시도 - 방ID: {}, 대상사용자ID: {}", roomId, targetUserId);
            throw new SecurityException("인증이 필요합니다");
        }

        Long requestUserId;
        try {
            requestUserId = Long.parseLong(principal);
        } catch (NumberFormatException e) {
            log.error("❌ 잘못된 사용자 ID 형식 - principal: {}", principal);
            throw new SecurityException("잘못된 인증 정보입니다");
        }

        // 본인 수정인 경우 허용
        if (requestUserId.equals(targetUserId)) {
            log.debug("✅ 본인 상태 변경 권한 확인 완료 - 사용자ID: {}, 오디오: {}, 비디오: {}",
                    requestUserId, request.getAudioEnabled(), request.getVideoEnabled());
            return;
        }

        // 방장 권한 확인 (다른 참가자 상태 변경)
        try {
            studyRoomValidator.validateOwnerPermission(roomId, requestUserId);
            log.info("✅ 방장 권한으로 다른 참가자 상태 변경 허용 - 방장ID: {}, 대상사용자ID: {}, 변경내용: {}",
                    requestUserId, targetUserId, request);

        } catch (Exception e) {
            log.warn("❌ 개인 참가자 상태 변경 권한 없음 - 요청자ID: {}, 대상사용자ID: {}, 오류: {}",
                    requestUserId, targetUserId, e.getMessage());
            throw new SecurityException("본인 또는 방장만 상태를 변경할 수 있습니다");
        }
    }

    // 개인 오디오 상태만 토글
    @PatchMapping("/{roomId}/participants/{userId}/audio/toggle")
    public ResponseEntity<UpdatePersonalStatusResponse> toggleParticipantAudio(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            log.info("개인 오디오 토글 요청 - 방ID: {}, 대상사용자ID: {}, 요청자: {}", roomId, userId, principal);

            // 본인 확인 또는 방장 권한 확인 (토글은 요청 객체가 없으므로 별도 검증)
            validateTogglePermission(roomId, userId, principal);

            // 오디오 상태 토글
            UpdatePersonalStatusResponse response = participantService.toggleAudioStatus(roomId, userId);

            log.info("✅ 개인 오디오 토글 성공 - 방ID: {}, 사용자: [{}], 오디오: {}",
                    roomId, response.getNickname(), response.getAudioEnabled());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.error("❌ 개인 오디오 토글 권한 없음 - 방ID: {}, 사용자ID: {}, 요청자: {}",
                    roomId, userId, authentication != null ? authentication.getPrincipal() : null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("❌ 개인 오디오 토글 실패 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 개인 비디오 상태만 토글
    @PatchMapping("/{roomId}/participants/{userId}/video/toggle")
    public ResponseEntity<UpdatePersonalStatusResponse> toggleParticipantVideo(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            log.info("개인 비디오 토글 요청 - 방ID: {}, 대상사용자ID: {}, 요청자: {}", roomId, userId, principal);

            // 본인 확인 또는 방장 권한 확인
            validateTogglePermission(roomId, userId, principal);

            // 비디오 상태 토글
            UpdatePersonalStatusResponse response = participantService.toggleVideoStatus(roomId, userId);

            log.info("✅ 개인 비디오 토글 성공 - 방ID: {}, 사용자: [{}], 비디오: {}",
                    roomId, response.getNickname(), response.getVideoEnabled());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.error("❌ 개인 비디오 토글 권한 없음 - 방ID: {}, 사용자ID: {}, 요청자: {}",
                    roomId, userId, authentication != null ? authentication.getPrincipal() : null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("❌ 개인 비디오 토글 실패 - 방ID: {}, 사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 토글 권한 검증
    private void validateTogglePermission(Long roomId, Long targetUserId, String principal) {

        if (principal == null || principal.trim().isEmpty()) {
            throw new SecurityException("인증이 필요합니다");
        }

        Long requestUserId;
        try {
            requestUserId = Long.parseLong(principal);
        } catch (NumberFormatException e) {
            throw new SecurityException("잘못된 인증 정보입니다");
        }

        // 본인 토글인 경우 허용
        if (requestUserId.equals(targetUserId)) {
            log.debug("✅ 본인 토글 권한 확인 완료 - 사용자ID: {}", requestUserId);
            return;
        }

        // 방장 권한 확인
        try {
            studyRoomValidator.validateOwnerPermission(roomId, requestUserId);
            log.info("✅ 방장 권한으로 다른 참가자 토글 허용 - 방장ID: {}, 대상사용자ID: {}",
                    requestUserId, targetUserId);

        } catch (Exception e) {
            throw new SecurityException("본인 또는 방장만 상태를 변경할 수 있습니다");
        }
    }

    // 참가자 강퇴 (방장만)
    @PostMapping("/{roomId}/participants/{userId}/ban")
    public ResponseEntity<Void> banParticipant(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            Authentication authentication) {

        try {
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            Long requestUserId = parseUserId(principal);

            log.info("참가자 강퇴 요청 - 방ID: {}, 대상사용자ID: {}, 요청자ID: {}", roomId, userId, requestUserId);

            // 1. 자기 자신 강퇴 방지 (400)
            if (requestUserId.equals(userId)) {
                throw new SelfBanAttemptException(roomId, userId);
            }

            // 2. 방장 권한 확인 (403)
            try {
                studyRoomValidator.validateOwnerPermission(roomId, requestUserId);
            } catch (Exception e) {
                throw new NotRoomOwnerException(roomId, requestUserId);
            }

            // 3. 대상이 참가자인지 확인 (404)
            if (!participantService.isUserInRoom(roomId, userId)) {
                throw new UserNotParticipantException(roomId, userId);
            }

            // 참가자 강퇴 처리
            participantService.banParticipant(roomId, userId);
            long remainingCount = participantService.getActiveParticipantCount(roomId);

            log.info("✅ 참가자 강퇴 성공 - 방ID: {}, 강퇴된사용자ID: {}, 방장ID: {}, 남은 참가자: {}명",
                    roomId, userId, requestUserId, remainingCount);

            return ResponseEntity.ok().build();

        } catch (SecurityException e) {
            log.error("❌ 참가자 강퇴 권한 없음 - 방ID: {}, 대상사용자ID: {}, 요청자: {}, 오류: {}",
                    roomId, userId, authentication != null ? authentication.getPrincipal() : null, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException | ParticipantException e) {
            log.error("❌ 참가자 강퇴 실패 (잘못된 요청) - 방ID: {}, 대상사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("❌ 참가자 강퇴 실패 - 방ID: {}, 대상사용자ID: {}, 오류: {}", roomId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ 참가자 강퇴 중 시스템 오류 - 방ID: {}, 대상사용자ID: {}, 오류: {}",
                    roomId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long parseUserId(String principal) {
        if (principal == null || principal.trim().isEmpty()) {
            throw new UnauthorizedException("인증이 필요합니다");
        }
        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("잘못된 인증 정보입니다");
        }
    }

    // 전체 음소거 제어 (방장만)
    @PostMapping("/{roomId}/mute-all")
    public ResponseEntity<MuteAllResponse> controlMuteAll(
            @PathVariable Long roomId,
            @RequestBody(required = false) MuteAllRequest request,
            Authentication authentication) {

        try {
            // 인증 확인
            String principal = authentication != null ? authentication.getPrincipal().toString() : null;
            if (principal == null || principal.trim().isEmpty()) {
                log.warn("❌ 인증되지 않은 전체 음소거 제어 시도 - 방ID: {}", roomId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Long requestUserId;
            try {
                requestUserId = Long.parseLong(principal);
            } catch (NumberFormatException e) {
                log.error("❌ 잘못된 사용자 ID 형식 - principal: {}", principal);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Request Body가 없으면 토글로 처리
            String action = (request != null && request.getAction() != null)
                    ? request.getAction().toLowerCase()
                    : "toggle";

            log.info("전체 음소거 제어 요청 - 방ID: {}, 방장ID: {}, 액션: {}", roomId, requestUserId, action);

            MuteAllResponse response;

            switch (action) {
                case "mute":
                    // 무조건 음소거 설정
                    response = participantService.muteAllParticipants(roomId, requestUserId);
                    break;

                case "unmute":
                    // 무조건 음소거 해제
                    response = participantService.unmuteAllParticipants(roomId, requestUserId);
                    break;

                case "toggle":
                default:
                    // 현재 상태 토글
                    response = participantService.toggleMuteAll(roomId, requestUserId);
                    break;
            }

            log.info("✅ 전체 음소거 제어 성공 - 방ID: {}, 방장ID: {}, 액션: {}, 결과상태: {}, 영향받은 참가자: {}명",
                    roomId, requestUserId, action, response.getIsAllMuted() ? "음소거" : "해제",
                    response.getIsAllMuted() ? response.getMutedParticipants() : response.getUnmutedParticipants());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.error("❌ 전체 음소거 제어 권한 없음 - 방ID: {}, 요청자: {}, 오류: {}",
                    roomId, authentication != null ? authentication.getPrincipal() : null, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException e) {
            log.error("❌ 전체 음소거 제어 실패 (잘못된 요청) - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (IllegalStateException e) {
            log.error("❌ 전체 음소거 제어 실패 (상태 오류) - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (RuntimeException e) {
            log.error("❌ 전체 음소거 제어 실패 - 방ID: {}, 오류: {}", roomId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("❌ 전체 음소거 제어 중 시스템 오류 - 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private void validateAuthentication(Authentication authentication, Long userId) {
        if (authentication == null) {
            throw new UnauthorizedException("인증이 필요합니다");
        }

        String principal;
        try {
            principal = authentication.getPrincipal().toString();
        } catch (Exception e) {
            throw new UnauthorizedException("인증 정보를 가져올 수 없습니다");
        }

        if (!principal.equals(userId.toString())) {
            throw new UnauthorizedException("본인만 접근할 수 있습니다");
        }
    }
}
