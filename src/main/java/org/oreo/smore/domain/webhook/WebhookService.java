package org.oreo.smore.domain.webhook;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final StudyRoomRepository studyRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final StudyRoomService studyRoomService;

    @Transactional
    public void handleParticipantLeft(String roomName, String identity) {
        if (roomName == null || identity == null) return;

        Optional<StudyRoom> roomOpt = studyRoomRepository.findByLiveKitRoomId(roomName);
        if (roomOpt.isEmpty()) return;

        Optional<User> userOpt = userRepository.findByNickname(identity);
        if (userOpt.isEmpty()) return;

        Long roomId = roomOpt.get().getRoomId();
        Long userId = userOpt.get().getUserId();

        if (roomOpt.get().getUserId().equals(userId)) {
            studyRoomService.deleteStudyRoom(roomId, userId);
        }

        List<Participant> targets =
                participantRepository.findAllByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId);

        if (targets.isEmpty()) return;

        for (Participant p : targets) {
            p.leave();
        }
        participantRepository.saveAll(targets);
    }
}
