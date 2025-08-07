package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StudyRoomTest {
    @Test
    @DisplayName("라이브킷 방 ID 설정 및 확인")
    void 라이브킷_방ID_설정_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(1L, 123L, "테스트 방", StudyRoomCategory.SELF_STUDY);

        // when
        studyRoom.setLiveKitRoomId("room_123");

        // then
        assertEquals("room_123", studyRoom.getLiveKitRoomId());
        assertTrue(studyRoom.hasLiveKitRoom());
    }

    @Test
    @DisplayName("라이브킷 방 ID 자동 생성")
    void 라이브킷_방ID_생성_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(1L, 456L, "테스트 방", StudyRoomCategory.EMPLOYMENT);

        // when
        String generatedRoomId = studyRoom.generateLiveKitRoomId();

        // then
        assertEquals("room_456", generatedRoomId);
    }

    @Test
    @DisplayName("라이브킷 방 ID 미설정 시 hasLiveKitRoom() false 반환")
    void 라이브킷_방ID_미설정_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(1L, 123L, "테스트 방", StudyRoomCategory.SELF_STUDY);

        // when & then
        assertFalse(studyRoom.hasLiveKitRoom());
    }

    @Test
    @DisplayName("라이브킷 방 ID 빈 문자열일 때 hasLiveKitRoom() false 반환")
    void 라이브킷_방ID_빈값일_때_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(1L, 123L, "테스트 방", StudyRoomCategory.SELF_STUDY);
        studyRoom.setLiveKitRoomId("");

        // when & then
        assertFalse(studyRoom.hasLiveKitRoom());
    }

    @Test
    @DisplayName("라이브킷 방 ID 공백 문자열일 때 hasLiveKitRoom() false 반환")
    void 라이브킷_방ID_공백일_때_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(1L, 123L, "테스트 방", StudyRoomCategory.SELF_STUDY);
        studyRoom.setLiveKitRoomId("   ");

        // when & then
        assertFalse(studyRoom.hasLiveKitRoom());
    }

    @Test
    @DisplayName("빌더로 생성 시 기본값 및 라이브킷 방 ID 설정 확인")
    void 빌더로_생성_테스트() {
        // given & when
        StudyRoom studyRoom = StudyRoom.builder()
                .userId(1L)
                .title("빌더 테스트 방")
                .category(StudyRoomCategory.CERTIFICATION)
                .liveKitRoomId("room_builder_test")
                .build();

        // then
        assertEquals("빌더 테스트 방", studyRoom.getTitle());
        assertEquals("room_builder_test", studyRoom.getLiveKitRoomId());
        assertTrue(studyRoom.hasLiveKitRoom());
        assertEquals(6, studyRoom.getMaxParticipants());
    }
}
