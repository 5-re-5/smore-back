package org.oreo.smore.domain.studytime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyTimeRepository extends JpaRepository<StudyTime, Long> {
    Optional<StudyTime> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<StudyTime> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<StudyTime> findAllByUserId(Long userId);

    List<Long> findDistinctUserIdsByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
