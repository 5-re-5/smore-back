package org.oreo.smore.domain.video.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.exception.MaxParticipantsExceededException;
import org.oreo.smore.domain.video.exception.StudyRoomNotFoundException;
import org.oreo.smore.domain.video.exception.WrongPasswordException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRoomValidator {
    // 스터디룸 입장 가능한지 체크 로직

    private final StudyRoomRepository studyRoomRepository;

    public StudyRoom validateRoomAccess(Long roomId, JoinRoomRequest request) {
        log.info("스터디룸 입장 검증 시작 - 방ID: {}, 사용자: {}", roomId, request.getIdentity());

        // 방 존재 여부 + 삭제 유무 확인
        StudyRoom studyRoom = validateRoomExists(roomId);

        // 비밀번호가 있을 때 비밀번호 검증
        validatePassword(studyRoom, request.getPassword());

        // 최대 인원 검증
        validateMaxParticipants(studyRoom);

        log.info("✅ 스터디룸 입장 검증 통과 - 방: [{}], 사용자: [{}]",
                studyRoom.getTitle(), request.getIdentity());

        return studyRoom;
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
    private void validateMaxParticipants(StudyRoom studyRoom) {
        log.debug("최대 인원 검증 - 최대인원: {}명", studyRoom.getMaxParticipants());

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
        boolean isOwner = studyRoom.getUserId().equals(userId);
        log.debug("방장 여부 확인 - 사용자ID: {}, 방장ID: {}, 결과: {}",
                userId, studyRoom.getUserId(), isOwner);
        return isOwner;
    }

    // 방 정보 로깅
    public void logRoomInfo(StudyRoom studyRoom) {
        log.info("방 정보 - ID: {}, 제목: [{}], 최대인원: {}명, 비밀번호: {}",
                studyRoom.getRoomId(),
                studyRoom.getTitle(),
                studyRoom.getMaxParticipants(),
                studyRoom.getPassword() != null ? "있음" : "없음");
    }
}
