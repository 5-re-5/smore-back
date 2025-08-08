package org.oreo.smore.domain.video.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.exception.*;
import org.oreo.smore.domain.video.service.UserIdentityService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRoomValidator {
    // 스터디룸 입장 가능한지 체크 로직

    private final StudyRoomRepository studyRoomRepository;
    private final UserIdentityService userIdentityService;

    public StudyRoom validateRoomAccess(Long roomId, JoinRoomRequest request, Long userId) {
        String userNickname = userIdentityService.generateIdentityForUser(userId);

        log.info("스터디룸 입장 검증 시작 - 방ID: {}, 닉네임: [{}], 사용자ID: {}", roomId, userNickname, userId);

        // 방 존재 여부 + 삭제 유무 확인
        StudyRoom studyRoom = validateRoomExists(roomId);

        // 방장 우선 입장 검증
        validateOwnerFirstEntry(studyRoom, userId);

        // 비밀번호가 있을 때 비밀번호 검증
        validatePassword(studyRoom, request.getPassword());

        // 최대 인원 검증
        validateMaxParticipants(studyRoom, userId);

        log.info("✅ 스터디룸 입장 검증 통과 - 방: [{}], 닉네임: [{}], 방장여부: [{}]",
                studyRoom.getTitle(), userNickname, isRoomOwner(studyRoom, userId));

        return studyRoom;
    }

    public StudyRoom validateRoomAccess(Long roomId, JoinRoomRequest request) {
        log.warn("⚠️ userId 없이 방 입장 검증 호출 - 방장 우선 입장 검증 스킵");

        StudyRoom studyRoom = validateRoomExists(roomId);
        validatePassword(studyRoom, request.getPassword());

        return studyRoom;
    }

    public StudyRoom validateRejoinAccess(Long roomId, Long userId) {
        String userNickname = userIdentityService.generateIdentityForUser(userId);

        log.info("재입장 검증 시작 - 방ID: {}, 닉네임: [{}], 사용자ID: {}", roomId, userNickname, userId);

        // 방 존재 여부 + 삭제 유무 확인
        StudyRoom studyRoom = validateRoomExists(roomId);

        log.info("✅ 재입장 검증 완료 - 방: [{}], 닉네임: [{}], 방장여부: [{}] (비밀번호 검증 생략)",
                studyRoom.getTitle(), userNickname, isRoomOwner(studyRoom, userId));

        return studyRoom;
    }

    // 방장 권한 검증
    public StudyRoom validateOwnerPermission(Long roomId, Long userId) {
        log.info("방장 권한 검증 - 방ID: {}, 사용자ID: {}", roomId, userId);

        StudyRoom studyRoom = validateRoomExists(roomId);
        validateIsRoomOwner(studyRoom, userId);

        log.info("✅ 방장 권한 검증 통과 - 방ID: {}, 방장ID: {}", roomId, userId);
        return studyRoom;
    }

    public void validateOwnerPermission(StudyRoom studyRoom, Long userId) {
        log.debug("방장 권한 검증 - 방ID: {}, 사용자ID: {}", studyRoom.getRoomId(), userId);
        validateIsRoomOwner(studyRoom, userId);
        log.debug("✅ 방장 권한 검증 통과");
    }

    private void validateIsRoomOwner(StudyRoom studyRoom, Long userId) {
        if (userId == null) {
            log.warn("❌ 사용자 ID가 null - 방장 권한 검증 실패");
            throw new OwnerPermissionRequiredException("사용자 정보가 없습니다.");
        }

        if (!isRoomOwner(studyRoom, userId)) {
            log.warn("❌ 방장 권한 없음 - 방ID: {}, 실제방장ID: {}, 요청사용자ID: {}",
                    studyRoom.getRoomId(), studyRoom.getUserId(), userId);
            throw new OwnerPermissionRequiredException("방장만 수행할 수 있는 작업입니다.");
        }
    }

    // 방 존재 여부 + 삭제 유무 확인
    private StudyRoom validateRoomExists(Long roomId) {
        log.debug("방 존재 여부 확인 - roomId: {}", roomId);

        StudyRoom studyRoom = studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> {
                    log.warn("❌ 존재하지 않거나 삭제된 방 접근 시도 - roomId: {}", roomId);
                    return new StudyRoomNotFoundException("방을 찾을 수 없습니다. roomId: " + roomId);
                });
        log.debug("✅ 방 존재 확인 완료 - 방제목: [{}]", studyRoom.getTitle());
        return studyRoom;
    }

    // 비밀번호가 있을 때 비밀번호 검증
    private void validatePassword(StudyRoom studyRoom, String inputPassword) {
        // 방에 비밀번호 설정 x -> 공개방
        if (studyRoom.getPassword() == null || studyRoom.getPassword().trim().isEmpty()) {
            log.debug("✅ 비밀번호 없는 공개방 - 검증 통과");
            return;
        }

        // 비밀번호가 있는 방인데 입력 안함
        if (inputPassword == null || inputPassword.trim().isEmpty()) {
            log.warn("❌ 비밀번호 필요한 방에 비밀번호 미입력 - roomId: {}", studyRoom.getRoomId());
            throw new WrongPasswordException("이 방은 비밀번호가 필요합니다.");
        }

        // 비밀번호 일치 여부 확인
        if (!studyRoom.getPassword().equals(inputPassword.trim())) {
            log.warn("❌ 잘못된 비밀번호 입력 - roomId: {}", studyRoom.getRoomId());
            throw new WrongPasswordException("비밀번호가 틀렸습니다.");
        }

        log.debug("✅ 비밀번호 검증 통과");
    }

    // 최대 인원 검증
    private void validateMaxParticipants(StudyRoom studyRoom, Long userId) {
        log.debug("최대 인원 검증 - 최대인원: {}명", studyRoom.getMaxParticipants());

        // 방장은 굳이 최대 인원 검증 안함
        if (isRoomOwner(studyRoom, userId)) {
            log.debug("방장은 굳이 최대 인원 검증 안함");
            return;
        }

        // 현재 참가자 수 조회하는 로직 필요함
        int currentParticipants = 1; // 일단 임시값

        if (currentParticipants >= studyRoom.getMaxParticipants()) {
            log.warn("❌ 최대 인원 초과 - 현재: {}명, 최대: {}명",
                    currentParticipants, studyRoom.getMaxParticipants());
            throw new MaxParticipantsExceededException(
                    String.format("방이 가득 찼습니다. (최대 %d명)", studyRoom.getMaxParticipants()));
        }

        log.debug("✅ 최대 인원 검증 통과 - 현재: {}명, 최대: {}명",
                currentParticipants, studyRoom.getMaxParticipants());

    }

    // 방장 여부 확인
    public boolean isRoomOwner(StudyRoom studyRoom, Long userId) {
        if (userId == null) {
            return false;
        }

        boolean isOwner = studyRoom.getUserId().equals(userId);
        log.debug("방장 여부 확인 - 사용자ID: {}, 방장ID: {}, 결과: {}",
                userId, studyRoom.getUserId(), isOwner);
        return isOwner;
    }

    // 방 정보 로깅
    public void logRoomInfo(StudyRoom studyRoom) {
        log.info("방 정보 - ID: {}, 제목: [{}], 최대인원: {}명, 비밀번호: {}, 방장입장: {}",
                studyRoom.getRoomId(),
                studyRoom.getTitle(),
                studyRoom.getMaxParticipants(),
                studyRoom.getPassword() != null ? "있음" : "없음",
                studyRoom.hasLiveKitRoom() ? "완료" : "대기중");
    }

    // 방장 우선 입장 검증
    private void validateOwnerFirstEntry(StudyRoom studyRoom, Long userId) {
        boolean isOwner = isRoomOwner(studyRoom, userId);
        boolean ownerHasJoined = studyRoom.hasLiveKitRoom();

        log.debug("방장 우선 입장 검증 - 방장여부: {}, 방장입장여부: {}", isOwner, ownerHasJoined);

        if (isOwner) {
            log.debug("======<방장 입장>======");
            return;
        }

        if (!ownerHasJoined) {
            log.warn("❌ 방장 미입장으로 인한 입장 거부 - 방ID: {}, 시도사용자ID: {}",
                    studyRoom.getRoomId(), userId);
            throw new OwnerNotJoinedException("방장이 아직 방에 입장하지 않았습니다. 방장이 먼저 입장한 후 참가하세요.");
        }

        log.debug("방장 이미 입장함 - 다른 참가자 입장 허용");
    }

}
