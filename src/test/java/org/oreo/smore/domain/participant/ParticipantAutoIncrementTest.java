package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ParticipantAutoIncrementTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParticipantRepository participantRepository;

    @Test
    void participant_저장시_ID_자동생성_테스트() {
        // Given
        Long roomId = 1L;
        Long userId = 2L;

        // When
        Participant participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        System.out.println("저장 전 participantId: " + participant.getParticipantId());

        // DB에 저장
        Participant saved = participantRepository.save(participant);
        entityManager.flush(); // 즉시 DB에 반영

        // Then
        System.out.println("저장 후 participantId: " + saved.getParticipantId());
        System.out.println("저장 후 joinedAt: " + saved.getJoinedAt());

        assertThat(saved.getParticipantId()).isNotNull();
        assertThat(saved.getParticipantId()).isGreaterThan(0L);
        assertThat(saved.getJoinedAt()).isNotNull();
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getUserId()).isEqualTo(userId);
    }

    @Test
    void 여러_participant_저장시_ID_증가_테스트() {
        // Given & When
        Participant p1 = participantRepository.save(
                Participant.builder().roomId(1L).userId(2L).build()
        );

        Participant p2 = participantRepository.save(
                Participant.builder().roomId(1L).userId(3L).build()
        );

        entityManager.flush();

        // Then
        System.out.println("첫 번째 participant ID: " + p1.getParticipantId());
        System.out.println("두 번째 participant ID: " + p2.getParticipantId());

        assertThat(p1.getParticipantId()).isNotNull();
        assertThat(p2.getParticipantId()).isNotNull();
        assertThat(p2.getParticipantId()).isGreaterThan(p1.getParticipantId());
    }
}