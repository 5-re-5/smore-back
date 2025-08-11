package org.oreo.smore.domain.point;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.studytime.StudyTime;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyPointScheduler {

    private static final String ATTENDANCE_REASON = "출석 포인트";
    private static final String STUDY_REASON = "공부시간 포인트";

    private final StudyTimeRepository studyTimeRepository;
    private final PointService pointService;
    private final PointRepository pointRepository;

    /**
     * 매일 00:00:00 (KST) 실행
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void distributeDailyPoints() {
        ZoneId KST = ZoneId.of("Asia/Seoul");

        LocalDate targetDate = LocalDate.now(KST).minusDays(1); // 어제
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEndExclusive = targetDate.plusDays(1).atStartOfDay(); // 배타 경계

        // 어제와 경계 겹치는 세션까지 포함해서 가져오기 (전일 00:00 ~ 익일 00:00)
        List<Long> userIds = studyTimeRepository.findDistinctUserIdsByCreatedAtBetween(
                dayStart.minusDays(1), dayEndExclusive.plusDays(1)
        );

        for (Long userId : userIds) {
            long totalSeconds = calcSecondsForDate(userId, dayStart, dayEndExclusive);
            long totalMinutes = totalSeconds / 60;
            int hours = (int) (totalSeconds / 3600);

            // (1) 출석 포인트: 60분 이상이면 5점
            if (totalMinutes >= 60) {
                // 중복 지급 방지: 동일 사유가 그 날짜에 이미 있으면 스킵
                boolean existsAttendance = pointRepository.existsByUserIdAndReasonAndTimestampBetween(
                        userId, ATTENDANCE_REASON, dayStart, dayEndExclusive.minusNanos(1)
                );
                if (!existsAttendance) {
                    pointService.addPoints(userId, 5, ATTENDANCE_REASON);
                }
            }

            // (2) 공부시간 포인트: 1시간당 1점 (정수 시간만)
            if (hours > 0) {
                boolean existsStudy = pointRepository.existsByUserIdAndReasonAndTimestampBetween(
                        userId, STUDY_REASON, dayStart, dayEndExclusive.minusNanos(1)
                );
                if (!existsStudy) {
                    pointService.addPoints(userId, hours, STUDY_REASON);
                }
            }
        }
    }

    /**
     * 주어진 사용자에 대해 target 일자 구간 [dayStart, dayEndExclusive) 에 겹치는 공부 시간을 초로 계산
     */
    private long calcSecondsForDate(Long userId, LocalDateTime dayStart, LocalDateTime dayEndExclusive) {
        List<StudyTime> records = studyTimeRepository.findAllByUserIdAndCreatedAtBetween(
                userId,
                dayStart.minusDays(1),     // 전날 시작 세션 포함
                dayEndExclusive.plusDays(1) // 익일로 넘어가는 세션 포함
        );

        long totalSeconds = 0L;
        LocalDateTime now = LocalDateTime.now();

        for (StudyTime r : records) {
            LocalDateTime from = r.getCreatedAt();
            LocalDateTime to = r.getDeletedAt();
            if (to == null || to.isAfter(now)) to = now;

            // 어제 구간으로 클램핑 (배타 경계 사용)
            LocalDateTime actualStart = from.isBefore(dayStart) ? dayStart : from;
            LocalDateTime actualEnd = to.isAfter(dayEndExclusive) ? dayEndExclusive : to;

            if (actualStart.isBefore(actualEnd)) {
                totalSeconds += ChronoUnit.SECONDS.between(actualStart, actualEnd);
            }
        }
        return totalSeconds;
    }
}