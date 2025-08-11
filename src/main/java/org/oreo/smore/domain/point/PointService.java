package org.oreo.smore.domain.point;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.point.dto.response.TotalPointsResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    public TotalPointsResponse getTotalPoints(Long userId) {
        return null;
    }
}
