package org.oreo.smore.domain.participant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final StudyRoomRepository studyRoomRepository;

    // 참가자 등록
    @Transactional
    public Participant joinRoom(Long roomId, Long userId) {
        log.info("참가자 등록 시작 - 방ID: {}, 사용자ID: {} ", roomId, userId);

        // 방 존재 여부 확인
        StudyRoom studyRoom = validateStudyRoomExists(roomId);

        // 이미 참가중인지 확인
        validateNotAlreadyInRoom(roomId, userId);

        // 방 최대 인원 확인
        validateRoomCapacity(studyRoom);

        // 참가자 엔티티 생성
        Participant participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        Participant savedParticipant = participantRepository.save(participant);

        long currentCount = participantRepository.countActiveParticipantsByRoomId(roomId);
        log.info("✅ 참가자 등록 완료 - 방ID: {}, 사용자ID: {}, 현재 참가자 수: {}/{}",
                roomId, userId, currentCount, studyRoom.getMaxParticipants());

        return savedParticipant;
    }

    // 방 최대 인원 검증
    private void validateRoomCapacity(StudyRoom studyRoom) {
        long currentCount = participantRepository.countActiveParticipantsByRoomId(studyRoom.getRoomId());

        if (currentCount >= studyRoom.getMaxParticipants()) {
            log.warn("방 정원 초과 - 방ID: {}, 현재: {}, 최대: {}",
                    studyRoom.getRoomId(), currentCount, studyRoom.getMaxParticipants());
            throw new ParticipantException.RoomFullException(
                    String.format("방이 가득함 (%d/%d)", currentCount, studyRoom.getMaxParticipants()));
        }

    }

    // 이미 참가중인지 검증
    private void validateNotAlreadyInRoom(Long roomId, Long userId) {
        if (isUserInRoom(roomId, userId)) {
            log.warn("이미 참가중인 사용자 - 방ID: {}, 사용자ID: {}", roomId, userId);
            throw new ParticipantException.AlreadyJoinedException(
                    String.format("사용자 %d는 이미 방 %d에 참가중입니다", userId, roomId));
        }
    }

    // 사용자가 특정 방에 참가중인지 확인
    private boolean isUserInRoom(Long roomId, Long userId) {
        List<Participant> activeParticipants = getActiveParticipants(roomId);
        boolean isInRoom = activeParticipants.stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        log.debug("사용자 방 참가 여부 - 방ID: {}, 사용자ID: {}, 참가중: {}", roomId, userId, isInRoom);
        return isInRoom;
    
    }

    private List<Participant> getActiveParticipants(Long roomId) {
        log.debug("현재 활성 참가자 조회 - 방ID: {}", roomId);
        List<Participant> activeParticipants = participantRepository.findActiveParticipantsByRoomId(roomId);
        log.debug("활성 참가자 수: {} - 방ID: {}", activeParticipants.size(), roomId);
        return activeParticipants;

    }

    // 스터디룸 존재 여부 검증
    private StudyRoom validateStudyRoomExists(Long roomId) {
        return studyRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 방 - 방ID: {}", roomId);
                    return new ParticipantException.StudyRoomNotFoundException(
                            String.format("방 %d를 찾을 수 없습니다", roomId));
                });
    }
}
