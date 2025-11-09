package org.oreo.smore.domain.point;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.point.dto.response.TotalPointsResponse;
import org.oreo.smore.domain.point.dto.response.OreoDrawResponse;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "user:totalPoints", key = "#userId")
    @Transactional(readOnly = true)
    public TotalPointsResponse getTotalPoints(Long userId) {
        return new TotalPointsResponse(pointRepository.sumDeltaByUserId(userId));
    }

    @CacheEvict(value = "user:totalPoints", key = "#userId")
    @Transactional
    public OreoDrawResponse drawOreo(Long userId) {
        long totalPoints = pointRepository.sumDeltaByUserId(userId);

        if (totalPoints < 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자가 존재하지 않습니다."));

        // 포인트 차감
        Point usedPoint = Point.builder()
                .userId(userId)
                .delta(-100)
                .reason("오레오 뽑기")
                .timestamp(LocalDateTime.now())
                .build();
        pointRepository.save(usedPoint);

        // 결과 랜덤 생성
        String result = new Random().nextBoolean() ? "O" : "RE";

        // user.level에 붙이기
        user.setLevel(user.getLevel() + result);
        userRepository.save(user);

        long updatedPoints = totalPoints - 100;

        return new OreoDrawResponse(result, user.getLevel(), updatedPoints);
    }
}
