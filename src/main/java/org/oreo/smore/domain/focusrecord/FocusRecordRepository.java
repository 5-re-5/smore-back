package org.oreo.smore.domain.focusrecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FocusRecordRepository extends JpaRepository<FocusRecord, Long> {
    List<FocusRecord> findByUserIdAndTimestampAfter(Long userId, Instant after);
}
