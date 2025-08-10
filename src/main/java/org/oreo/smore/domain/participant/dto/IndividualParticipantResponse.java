package org.oreo.smore.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualParticipantResponse {

    private Long userId;
    private String nickname;
    private Boolean isOwner;
    private Boolean audioEnabled;
    private Boolean videoEnabled;
    private Integer todayStudyTime;
    private Integer targetStudyTime;

    private Boolean isInRoom;
    private String roomName;
    private Integer totalParticipants;

    public static IndividualParticipantResponse fromParticipantInfo(
            ParticipantInfo participantInfo,
            String roomName,
            Integer totalParticipants) {

        return IndividualParticipantResponse.builder()
                .userId(participantInfo.getUserId())
                .nickname(participantInfo.getNickname())
                .isOwner(participantInfo.getIsOwner())
                .audioEnabled(participantInfo.getAudioEnabled())
                .videoEnabled(participantInfo.getVideoEnabled())
                .todayStudyTime(participantInfo.getTodayStudyTime())
                .targetStudyTime(participantInfo.getTargetStudyTime())
                .isInRoom(true)
                .roomName(roomName)
                .totalParticipants(totalParticipants)
                .build();
    }

    @Override
    public String toString() {
        return String.format(
                "IndividualParticipantResponse{userId=%d, nickname='%s', isOwner=%s, audioEnabled=%s, videoEnabled=%s, " +
                        "todayStudyTime=%d, targetStudyTime=%d, isInRoom=%s, roomName='%s', totalParticipants=%d}",
                userId, nickname, isOwner, audioEnabled, videoEnabled,
                todayStudyTime, targetStudyTime, isInRoom, roomName, totalParticipants);
    }

}
