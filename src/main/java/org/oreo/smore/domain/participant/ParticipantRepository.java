package org.oreo.smore.domain.participant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    long countByRoomIdAndLeftAtIsNull(Long roomId);
}
