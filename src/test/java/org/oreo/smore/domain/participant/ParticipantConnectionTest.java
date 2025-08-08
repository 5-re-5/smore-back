package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ParticipantConnectionTest {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Test
    void joinRoom_성공_테스트() {
        // Given: 먼저 StudyRoom 생성
        StudyRoom studyRoom = StudyRoom.builder()
                .title("테스트 방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(10)
                .userId(1L)  // 방장 ID
                .build();

        StudyRoom savedRoom = studyRoomRepository.save(studyRoom);
        Long roomId = savedRoom.getRoomId();
        Long userId = 2L;

        try {
            // When
            System.out.println("=== ParticipantService.joinRoom 시작 ===");
            System.out.println("생성된 방 ID: " + roomId);

            Participant participant = participantService.joinRoom(roomId, userId);

            // Then
            System.out.println("✅ 참가자 등록 성공!");
            System.out.println("participantId: " + participant.getParticipantId());
            System.out.println("roomId: " + participant.getRoomId());
            System.out.println("userId: " + participant.getUserId());
            System.out.println("joinedAt: " + participant.getJoinedAt());
            System.out.println("audioEnabled: " + participant.isAudioEnabled());
            System.out.println("videoEnabled: " + participant.isVideoEnabled());

        } catch (Exception e) {
            System.out.println("❌ 참가자 등록 실패!");
            System.out.println("에러 타입: " + e.getClass().getSimpleName());
            System.out.println("에러 메시지: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("원인: " + e.getCause().getMessage());
            }
            e.printStackTrace();

            throw e;
        }
    }
}