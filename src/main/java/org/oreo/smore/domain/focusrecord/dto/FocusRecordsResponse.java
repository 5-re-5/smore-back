package org.oreo.smore.domain.focusrecord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FocusRecordsResponse {
    private DataWrapper data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataWrapper {
        private AiInsightsDto aiInsights;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AiInsightsDto {
        private String feedback;

        private FocusTimeDto bestFocusTime;

        private FocusTimeDto worstFocusTime;

        private Integer averageFocusDuration;

        private FocusTrackDto focusTrack;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FocusTimeDto {
        private String start;
        private String end;

        private Integer avgFocusScore;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FocusTrackDto {
        private List<String> labels;
        private List<Integer> scores;
    }
}