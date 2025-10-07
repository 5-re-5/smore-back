package org.oreo.smore.domain.focusrecord;

import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse.AiInsightsDto;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse.FocusTimeDto;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse.FocusTrackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FocusRecordService {

    private static final int WINDOW_HOURS = 2;
    private static final int FOCUS_THRESHOLD = 80;
    private static final double MINUTES_PER_SEGMENT = 2.0;

    private static final List<String> HOUR_LABELS = IntStream.range(0, 24)
            .mapToObj(hour -> String.format("%02d", hour))
            .toList();

    private final FocusRecordRepository focusRecordRepository;
    private final FocusFeedbackService focusFeedbackService;


    public FocusRecordsResponse getFocusRecords(Long userId, String timeZoneOffset) {
        ZoneOffset clientOffset = ZoneOffset.of(timeZoneOffset);

        List<FocusRecord> records = loadLastMonthRecords(userId);
        HourlyStats stats = calculateHourlyStats(records, clientOffset);

        FocusTrackDto trackDto = buildFocusTrack(stats);
        FocusTimeDto bestWindow = findWindow(stats, true);
        FocusTimeDto worstWindow = findWindow(stats, false);
        int averageDurationMinutes = calculateAverageFocusDuration(records);

        String feedback = generateFeedback(bestWindow, worstWindow, averageDurationMinutes, trackDto);

        AiInsightsDto insights = new AiInsightsDto(
                feedback, bestWindow, worstWindow, averageDurationMinutes, trackDto
        );
        return new FocusRecordsResponse(insights);
    }

    private List<FocusRecord> loadLastMonthRecords(Long userId) {
        Instant oneMonthAgo = LocalDateTime.now()
                .minusMonths(1)
                .toInstant(ZoneOffset.UTC);

        List<FocusRecord> records =
                focusRecordRepository.findByUserIdAndTimestampAfter(userId, oneMonthAgo);

        return records;
    }

    private HourlyStats calculateHourlyStats(
            List<FocusRecord> records, ZoneOffset offset) {

        Map<Integer, List<FocusRecord>> groupedByHour = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTimestamp().atOffset(offset).getHour()
                ));

        Map<Integer, Double> hourlyAverages = new HashMap<>();
        Map<Integer, Long> recordCounts = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            List<FocusRecord> bucket = groupedByHour.getOrDefault(hour, Collections.emptyList());
            recordCounts.put(hour, (long) bucket.size());
            double avgStatus = bucket.stream()
                    .mapToInt(FocusRecord::getStatus)
                    .average()
                    .orElse(0.0);
            hourlyAverages.put(hour, avgStatus);
        }
        return new HourlyStats(hourlyAverages, recordCounts);
    }

    private FocusTrackDto buildFocusTrack(HourlyStats stats) {
        List<Integer> roundedScores = HOUR_LABELS.stream()
                .map(Integer::parseInt)
                .map(hour -> (int) Math.round(stats.hourlyAverages().get(hour)))
                .toList();

        return new FocusTrackDto(HOUR_LABELS, roundedScores);
    }

    private FocusTimeDto findWindow(HourlyStats stats, boolean findMax) {
        double bestValue = findMax
                ? Double.NEGATIVE_INFINITY
                : Double.POSITIVE_INFINITY;
        int bestStart = 0;

        for (int startHour = 0; startHour <= 24 - WINDOW_HOURS; startHour++) {
            OptionalDouble windowAvgOpt = calculateWindowAverage(stats, startHour);
            if (windowAvgOpt.isEmpty()) {
                continue;
            }
            double windowAvg = windowAvgOpt.getAsDouble();
            boolean isBetter = findMax
                    ? windowAvg > bestValue
                    : windowAvg < bestValue;

            if (isBetter) {
                bestValue = windowAvg;
                bestStart = startHour;
            }
        }

        String from = String.format("%02d:00", bestStart);
        String to = String.format("%02d:00", bestStart + WINDOW_HOURS);
        return new FocusTimeDto(from, to, (int) Math.round(bestValue));
    }

    private OptionalDouble calculateWindowAverage(HourlyStats stats, int startHour) {
        double sum = 0;
        for (int offset = 0; offset < WINDOW_HOURS; offset++) {
            int hour = startHour + offset;
            if (stats.recordCounts().get(hour) == 0L) {
                return OptionalDouble.empty();
            }
            sum += stats.hourlyAverages().get(hour);
        }
        return OptionalDouble.of(sum / WINDOW_HOURS);
    }

    private int calculateAverageFocusDuration(List<FocusRecord> records) {
        records.sort(Comparator.comparing(FocusRecord::getTimestamp));

        List<Integer> segments = new ArrayList<>();
        int currentStreak = 0;

        for (FocusRecord record : records) {
            if (record.getStatus() >= FOCUS_THRESHOLD) {
                currentStreak++;
            } else if (currentStreak > 0) {
                segments.add(currentStreak);
                currentStreak = 0;
            }
        }
        if (currentStreak > 0) {
            segments.add(currentStreak);
        }

        if (segments.isEmpty()) {
            return 0;
        }
        double averageSegments = segments.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return (int) Math.round(averageSegments * MINUTES_PER_SEGMENT);
    }

    private String generateFeedback(
            FocusTimeDto best, FocusTimeDto worst,
            int avgDuration, FocusTrackDto track) {
        // return focusFeedbackService.generateOneLineFeedback(best, worst, avgDuration, track);
        // 임시: 기본 피드백 직접 반환
        return "훌륭해요! 꾸준히 이어가면 분명 좋은 결과가 있을 거예요😊";
    }

    private record HourlyStats(
            Map<Integer, Double> hourlyAverages,
            Map<Integer, Long> recordCounts
    ) {
    }
}
