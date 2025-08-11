package org.oreo.smore.domain.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuteAllResponse {

    private Long roomId;
    private Boolean isAllMuted;
    private Integer totalParticipants;
    private Integer mutedParticipants;
    private Integer unmutedParticipants;
    private String message;                 // 결과 메시지
    private Long performedBy;               // 수행한 사용자 ID (방장)

    // 설정 성공 응답 생성
    public static MuteAllResponse muteSuccess(Long roomId, Integer totalParticipants,
                                              Integer mutedCount, Long ownerId) {
        return MuteAllResponse.builder()
                .roomId(roomId)
                .isAllMuted(true)
                .totalParticipants(totalParticipants)
                .mutedParticipants(mutedCount)
                .message(String.format("전체 음소거가 설정되었습니다 (%d명 음소거)", mutedCount))
                .performedBy(ownerId)
                .build();
    }

    // 해제 성공 응답 생성
    public static MuteAllResponse unmuteSuccess(Long roomId, Integer totalParticipants,
                                                Integer unmutedCount, Long ownerId) {
        return MuteAllResponse.builder()
                .roomId(roomId)
                .isAllMuted(false)
                .totalParticipants(totalParticipants)
                .unmutedParticipants(unmutedCount)
                .message(String.format("전체 음소거가 해제되었습니다 (%d명 해제)", unmutedCount))
                .performedBy(ownerId)
                .build();
    }

    // 로깅용 toString
    @Override
    public String toString() {
        return String.format(
                "MuteAllResponse{roomId=%d, isAllMuted=%s, totalParticipants=%d, " +
                        "mutedParticipants=%s, unmutedParticipants=%s, message='%s', performedBy=%d}",
                roomId, isAllMuted, totalParticipants,
                mutedParticipants, unmutedParticipants, message, performedBy);
    }
}