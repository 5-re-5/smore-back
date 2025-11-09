package org.oreo.smore.domain.studytime;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.studytime.dto.response.StudyTimeStatisticsResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StudyTimeService {

    private final StudyTimeRepository studyTimeRepository;

    /**
     * 공부 시작
     */
    @CacheEvict(value = "study:stats", key = "#userId")
    public void startStudyTime(Long userId) {
        StudyTime studyTime = StudyTime.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now()) // 서버에서 현재 시간 저장
                .deletedAt(LocalDateTime.now()) // 서버에서 현재 시간 저장
                .build();

        studyTimeRepository.save(studyTime);
    }

    @CacheEvict(value = "study:stats", key = "#userId")
    public void updateStudyTime(Long userId) {
        StudyTime latestStudyTime = studyTimeRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 공부 기록이 없습니다."));

        latestStudyTime.setDeletedAt(LocalDateTime.now());
        studyTimeRepository.save(latestStudyTime);
    }

    @Cacheable(value = "study:stats", key = "#userId")
    public StudyTimeStatisticsResponse getStatistics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate oneYearAgo = today.minusYears(1);

        // 1. 전체 기록 조회 (연속 출석은 전체 범위 필요)
        List<StudyTime> allRecords = studyTimeRepository.findAllByUserId(userId);

        // 일자별 초 단위 집계로 변경 (분 손실 방지)
        Map<LocalDate, Long> dailySeconds = new HashMap<>();

        for (StudyTime record : allRecords) {
            LocalDateTime from = record.getCreatedAt();
            LocalDateTime to = record.getDeletedAt();
            if (to == null) to = LocalDateTime.now(); // 진행 중 세션 처리

            // 방어: 역전 구간 스킵
            if (!from.isBefore(to)) continue;

            for (LocalDate date = from.toLocalDate();
                 !date.isAfter(to.toLocalDate());
                 date = date.plusDays(1)) {

                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEndExclusive = date.plusDays(1).atStartOfDay(); // 00:00 (배타)

                LocalDateTime actualStart = from.isAfter(dayStart) ? from : dayStart;
                LocalDateTime actualEnd = to.isBefore(dayEndExclusive) ? to : dayEndExclusive;

                if (actualStart.isBefore(actualEnd)) {
                    long seconds = Duration.between(actualStart, actualEnd).getSeconds();
                    dailySeconds.merge(date, seconds, Long::sum);
                }
            }
        }

        Map<LocalDate, Integer> dailyMinutes = new HashMap<>();
        dailySeconds.forEach((d, sec) -> dailyMinutes.put(d, (int) Math.floorDiv(sec, 60)));      // 버림
// 또는 반올림: (int) Math.round(sec / 60.0)


        // 연속 출석 계산 (전체 기록 기준)
        int attendanceStreak = 0;
        for (int i = 1; ; i++) {
            LocalDate date = today.minusDays(i);
            if (dailyMinutes.getOrDefault(date, 0) >= 60) attendanceStreak++;
            else break;
        }
        if (dailyMinutes.getOrDefault(today, 0) >= 60) attendanceStreak++; // 오늘 출석했으면 포함

        // weekdayGraph (이번 주 월~일)
        int[] weekdayGraph = new int[7];
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            int dow = date.getDayOfWeek().getValue() % 7;
            int minutes = dailyMinutes.getOrDefault(date, 0);
            weekdayGraph[dow] = minutes / 60;
        }

        // weeklyGraph (최근 5주)
        List<Integer> weeklyGraph = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            LocalDate start = monday.minusWeeks(i);
            LocalDate end = start.plusDays(6);
            int sumMinutes = 0;
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                sumMinutes += dailyMinutes.getOrDefault(date, 0);
            }
            weeklyGraph.add(sumMinutes / 60);
        }


        // studyTrack (최근 1년)
        List<StudyTimeStatisticsResponse.Point> points =
                Stream.iterate(oneYearAgo, date -> date.plusDays(1))
                        .limit(ChronoUnit.DAYS.between(oneYearAgo, today) + 1) // 최근 1년 날짜 전부
                        .map(date -> new StudyTimeStatisticsResponse.Point(
                                date.toString(),
                                dailyMinutes.getOrDefault(date, 0) // 없으면 0
                        ))
                        .toList();

        return StudyTimeStatisticsResponse.builder()
                .userId(userId)
                .totalAttendance(attendanceStreak)
                .weekdayGraph(Arrays.stream(weekdayGraph).boxed().toList())
                .weeklyGraph(weeklyGraph)
                .studyTrack(new StudyTimeStatisticsResponse.StudyTrack(points))
                .build();
    }
}
