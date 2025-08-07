// src/main/java/org/oreo/smore/domain/studyroom/dto/StudyRoomDto.java
package org.oreo.smore.domain.studyroom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
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
public class StudyRoomDto implements Identifiable {
    private Long roomId;

    private String title;

    private String thumbnailUrl;

    private List<String> tag;

    private String category;

    private Integer maxParticipants;

    private Long currentParticipants;

    private String password;

    private String createdAt;

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
    public static StudyRoomDto of(
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

        return new StudyRoomDto(
                e.getRoomId(),
                e.getTitle(),
                e.getThumbnailUrl(),
                tags,
                e.getCategory().name(),         // enum → String
                e.getMaxParticipants(),
                currentParticipants,
                e.getPassword(),
                created,
                new CreatorDto(creatorNickname)
        );
    }
}
