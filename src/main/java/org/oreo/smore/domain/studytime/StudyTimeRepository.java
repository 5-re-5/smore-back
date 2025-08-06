package org.oreo.smore.domain.studytime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyTimeRepository extends JpaRepository<StudyTime, Long> {
    Optional<StudyTime> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
