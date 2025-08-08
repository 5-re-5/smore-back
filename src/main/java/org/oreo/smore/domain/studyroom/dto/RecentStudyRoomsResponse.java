package org.oreo.smore.domain.studyroom.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecentStudyRoomsResponse {
    private Data data;

    @Getter
    @Builder
    public static class Data {
        private List<RoomDto> rooms;
    }

    @Getter
    @Builder
    public static class RoomDto {
        private Long roomId;
        private String title;
        private String owner; // 방장 닉네임
        private String category;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private String tag;
        private String thumbnailUrl;
        private Boolean isDeleted;
    }
}