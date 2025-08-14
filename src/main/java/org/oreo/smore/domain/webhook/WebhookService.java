package org.oreo.smore.domain.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.participant.Participant;
import org.oreo.smore.domain.participant.ParticipantRepository;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomService;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final StudyRoomRepository studyRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final StudyRoomService studyRoomService;

    @Transactional
    public int handleParticipantLeft(String roomName, String identity) {
        if (roomName == null || identity == null) return 0;

        Optional<StudyRoom> roomOpt = studyRoomRepository.findByLiveKitRoomId(roomName);
        if (roomOpt.isEmpty()) return 0;

        Optional<User> userOpt = userRepository.findByNickname(identity);
        if (userOpt.isEmpty()) return 0;

        Long roomId = roomOpt.get().getRoomId();
        Long userId = userOpt.get().getUserId();
        boolean isOwner = roomOpt.get().getUserId().equals(userId);

        log.info("LiveKit participant_left 웹훅 - 방ID: {}, 사용자ID: {}, 방장여부: {}",
                roomId, userId, isOwner);

        // 핵심: 이미 나간 상태면 중복 이벤트로 판단 (새로고침 가능성)
        List<Participant> activeParticipants = participantRepository.findAllByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId);
        if (activeParticipants.isEmpty()) {
            log.info("✅ 이미 나간 참가자 - 중복 이벤트로 무시 - 방ID: {}, 사용자ID: {}",
                    roomId, userId);
            return 0; // 이미 처리됨 - 새로고침으로 판단
        }

        // 실제 나가기 처리
        if (isOwner) {
            // 방장 나가기 → 방 삭제
            log.warn("방장 나가기 - 방 삭제 처리 - 방ID: {}, 방장ID: {}", roomId, userId);
            try {
                studyRoomService.deleteStudyRoomByOwnerLeave(roomId, userId);
                return 1;
            } catch (Exception e) {
                log.error("❌ 방 삭제 실패 - 방ID: {}, 방장ID: {}, 오류: {}", roomId, userId, e.getMessage());
                return 0;
            }
        } else {
            // 일반 참가자 나가기
            for (Participant p : activeParticipants) {
                p.leave();
            }
            participantRepository.saveAll(activeParticipants);

            log.info("✅ 일반 참가자 나가기 완료 - 방ID: {}, 사용자ID: {}", roomId, userId);
            return 0;
        }
    }
}