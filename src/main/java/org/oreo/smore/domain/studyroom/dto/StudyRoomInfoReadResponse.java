package org.oreo.smore.domain.studyroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.oreo.smore.global.common.CursorPage.Identifiable;
import org.oreo.smore.domain.studyroom.StudyRoom;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@ToString
public class StudyRoomInfoReadResponse implements Identifiable {
    private Long roomId;

    private String title;

    private String description;

    private String thumbnailUrl;

    private List<String> tag;

    private String category;

    private Integer maxParticipants;

    private Long currentParticipants;

    private String createdAt;

    private Boolean isPomodoro;

    private Boolean isPrivate;

    private CreatorDto creator;

    @Override
    public Long getId() {
        return roomId;
    }

    @Getter
    @AllArgsConstructor
    public static class CreatorDto {
        private String nickname;
    }

    /**
     * 엔티티 + 집계값 → DTO
     */
    public static StudyRoomInfoReadResponse of(
            StudyRoom e,
            long       currentParticipants,
            String     creatorNickname
    ) {
        List<String> tags = e.getTag() == null
                ? Collections.emptyList()
                : Arrays.asList(e.getTag().split(","));
        String created = e.getCreatedAt()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        return new StudyRoomInfoReadResponse(
                e.getRoomId(),
                e.getTitle(),
                e.getDescription(),
                e.getThumbnailUrl(),
                tags,
                e.getCategory().name(),         // enum → String
                e.getMaxParticipants(),
                currentParticipants,
                created,
                !(e.getFocusTime() == null),
                !(e.getPassword() == null || e.getPassword().isEmpty()),
                new CreatorDto(creatorNickname)
        );
    }
}
