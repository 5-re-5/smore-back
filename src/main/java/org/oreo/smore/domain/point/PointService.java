package org.oreo.smore.domain.point;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.point.dto.response.TotalPointsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public TotalPointsResponse getTotalPoints(Long userId) {
        return new TotalPointsResponse(pointRepository.sumDeltaByUserId(userId));
    }
}
