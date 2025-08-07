package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // application-test.yml 사용
class ParticipantJoinedAtTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParticipantRepository participantRepository;

    @Test
    void participant_생성시_joinedAt_자동설정_테스트() {
        // Given
        Long roomId = 1L;
        Long userId = 2L;

        // When
        Participant participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        // 저장 전 joinedAt 확인
        System.out.println("저장 전 joinedAt: " + participant.getJoinedAt());

        // DB에 저장
        Participant saved = participantRepository.save(participant);
        entityManager.flush(); // 즉시 DB에 반영

        // Then
        System.out.println("저장 후 joinedAt: " + saved.getJoinedAt());
        assertThat(saved.getJoinedAt()).isNotNull();
        assertThat(saved.getParticipantId()).isNotNull();
    }
}