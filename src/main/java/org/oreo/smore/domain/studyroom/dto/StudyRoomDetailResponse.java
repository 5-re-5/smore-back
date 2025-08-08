package org.oreo.smore.domain.studyroom.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyRoomDetailResponse {
    private Data data;

    @Getter
    @Builder
    public static class Data {
        private Long roomId;
        private String title;
        private String description;
        private String thumbnailUrl;
        private String tag;
        private String category;
        private Integer focusTime;
        private Integer breakTime;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private String createdAt;
        private CreatorDto creator;
    }

    @Getter
    @Builder
    public static class CreatorDto {
        private Long userId;
        private String nickname;
    }
}