package org.oreo.smore.domain.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Participant 엔티티 테스트")
class ParticipantTest {

    private Long roomId;
    private Long userId;
    private Participant participant;

    @BeforeEach
    void setUp() {
        roomId = 1L;
        userId = 123L;
        participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        System.out.println("🔧 테스트 준비 완료 - 방ID: " + roomId + ", 사용자ID: " + userId);
    }

    @Test
    @DisplayName("참가자 객체 생성 시 기본값 설정 확인")
    void 참가자_객체_생성_시_기본값_설정_확인() {
        // When & Then
        assertThat(participant.getRoomId()).isEqualTo(roomId);
        assertThat(participant.getUserId()).isEqualTo(userId);
        // 오디오/비디오 기본값 확인
        assertThat(participant.isAudioEnabled()).isTrue();
        assertThat(participant.isVideoEnabled()).isTrue();
        // 음소거/강퇴 상태 확인
        assertThat(participant.isMutedInRoom()).isFalse();
        assertThat(participant.isBannedFromRoom()).isFalse();
        // 시간 필드 확인
        assertThat(participant.getLeftAt()).isNull();
        assertThat(participant.getJoinedAt()).isNull(); // @CreatedDate는 실제 저장시에만 설정됨

        System.out.println("✅ 참가자 기본값 설정 확인 완료");
        System.out.println("   - 오디오 상태: " + participant.isAudioEnabled());
        System.out.println("   - 비디오 상태: " + participant.isVideoEnabled());
        System.out.println("   - 음소거 상태: " + participant.isMutedInRoom());
        System.out.println("   - 강퇴 상태: " + participant.isBannedFromRoom());
        System.out.println("   - 퇴장 시간: " + participant.getLeftAt());
    }

    @Test
    @DisplayName("참가자 음소거 설정 테스트")
    void 참가자_음소거_설정_테스트() {
        // Given
        assertThat(participant.isAudioEnabled()).isTrue();

        // When
        participant.mute();

        // Then
        assertThat(participant.isMutedInRoom()).isTrue();
        assertThat(participant.isAudioEnabled()).isFalse();

        System.out.println("✅ 참가자 음소거 설정 테스트 완료");
        System.out.println("   - 음소거 상태: " + participant.isMutedInRoom());
    }

    @Test
    @DisplayName("참가자 음소거 해제 테스트")
    void 참가자_음소거_해제_테스트() {
        // Given
        participant.mute();
        assertThat(participant.isMutedInRoom()).isTrue();

        // When
        participant.unmute();

        // Then
        assertThat(participant.isMutedInRoom()).isFalse();
        assertThat(participant.isAudioEnabled()).isTrue();

        System.out.println("✅ 참가자 음소거 해제 테스트 완료");
        System.out.println("   - 오디오 상태: " + participant.isAudioEnabled());
    }

    @Test
    @DisplayName("방 존재 여부 확인 - 정상 참가자")
    void 방_존재_여부_확인_정상_참가자() {
        // When & Then
        assertThat(participant.isInRoom()).isTrue();

        System.out.println("✅ 정상 참가자 방 존재 확인 완료");
        System.out.println("   - 방에 있음: " + participant.isInRoom());
    }

    @Test
    @DisplayName("방 존재 여부 확인 - 퇴장한 참가자")
    void 방_존재_여부_확인_퇴장한_참가자() {
        // Given
        participant.leave();

        // When & Then
        assertThat(participant.isInRoom()).isFalse();

        System.out.println("✅ 퇴장한 참가자 방 존재 확인 완료");
        System.out.println("   - 방에 있음: " + participant.isInRoom());
    }

    @Test
    @DisplayName("방 존재 여부 확인 - 강퇴당한 참가자")
    void 방_존재_여부_확인_강퇴당한_참가자() {
        // Given
        participant.ban();

        // When & Then
        assertThat(participant.isInRoom()).isFalse();
        assertThat(participant.isBannedFromRoom()).isTrue();

        System.out.println("✅ 강퇴당한 참가자 방 존재 확인 완료");
        System.out.println("   - 방에 있음: " + participant.isInRoom());
        System.out.println("   - 강퇴됨: " + participant.isBannedFromRoom());
    }

    @Test
    @DisplayName("강퇴 후 음소거 상태 변경 불가 테스트")
    void 강퇴_후_음소거_상태_변경_테스트() {
        // Given
        participant.ban();
        assertThat(participant.isBannedFromRoom()).isTrue();

        // When - 강퇴된 상태에서 음소거 설정 시도
        participant.mute();

        // Then - 음소거는 설정되지만 방에는 없는 상태
        assertThat(participant.isMutedInRoom()).isTrue();
        assertThat(participant.isInRoom()).isFalse();

        System.out.println("✅ 강퇴 후 음소거 상태 변경 테스트 완료");
        System.out.println("   - 강퇴 상태: " + participant.isBannedFromRoom());
        System.out.println("   - 음소거 상태: " + participant.isMutedInRoom());
        System.out.println("   - 방에 있음: " + participant.isInRoom());
    }
}
