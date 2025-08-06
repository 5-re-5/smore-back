package org.oreo.smore.domain.studyroom.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
public class CreateStudyRoomResponse {

    private Long roomId;
    private String title;
    private String description;
    private boolean hasPassword;
    private Integer maxParticipants;
    private String tag;
    private StudyRoomCategory category;
    private Integer focusTime;
    private Integer breakTime;
    private String inviteHashCode;
    private LocalDateTime createdAt;
    private Long ownerId;

    public CreateStudyRoomResponse(Long roomId, String title, String description,
                                   boolean hasPassword, Integer maxParticipants, String tag,
                                   StudyRoomCategory category, Integer focusTime, Integer breakTime,
                                   String inviteHashCode, LocalDateTime createdAt, Long ownerId) {
        this.roomId = roomId;
        this.title = title;
        this.description = description;
        this.hasPassword = hasPassword;
        this.maxParticipants = maxParticipants;
        this.tag = tag;
        this.category = category;
        this.focusTime = focusTime;
        this.breakTime = breakTime;
        this.inviteHashCode = inviteHashCode;
        this.createdAt = createdAt;
        this.ownerId = ownerId;
    }

    public static CreateStudyRoomResponse from(org.oreo.smore.domain.studyroom.StudyRoom studyRoom) {
        return CreateStudyRoomResponse.builder()
                .roomId(studyRoom.getRoomId())
                .title(studyRoom.getTitle())
                .description(studyRoom.getDescription())
                .hasPassword(studyRoom.getPassword() != null && !studyRoom.getPassword().trim().isEmpty())
                .maxParticipants(studyRoom.getMaxParticipants())
                .tag(studyRoom.getTag())
                .category(studyRoom.getCategory())
                .focusTime(studyRoom.getFocusTime())
                .breakTime(studyRoom.getBreakTime())
                .inviteHashCode(studyRoom.getInviteHashCode())
                .createdAt(studyRoom.getCreatedAt())
                .ownerId(studyRoom.getUserId())
                .build();
    }
}
