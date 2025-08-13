package org.oreo.smore.domain.point;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointRepository extends JpaRepository<Point, Long> {

    @Query("select coalesce(sum(p.delta), 0) from Point p where p.userId = :userId")
    long sumDeltaByUserId(@Param("userId") Long userId);
}
