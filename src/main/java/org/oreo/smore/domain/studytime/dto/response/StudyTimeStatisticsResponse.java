package org.oreo.smore.domain.studytime.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyTimeStatisticsResponse {
    private Long userId;
    private Integer totalAttendance;
    private List<Integer> weekdayGraph;
    private List<Integer> weeklyGraph;
    private StudyTrack studyTrack;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudyTrack {
        private List<Point> points;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private String date;
        private Integer minutes;
    }
}