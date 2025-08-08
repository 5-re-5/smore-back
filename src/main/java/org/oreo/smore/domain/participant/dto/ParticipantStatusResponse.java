package org.oreo.smore.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatusResponse {
    private List<ParticipantInfo> participants;
    private RoomInfo roomInfo;
}
