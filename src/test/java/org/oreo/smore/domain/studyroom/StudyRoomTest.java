package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class StudyRoomTest {

    @Test
    @DisplayName("라이브킷 방 ID 설정 및 확인")
    void 라이브킷_방ID_설정_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(123L, 1L, "테스트 방", StudyRoomCategory.SELF_STUDY);

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
        StudyRoom studyRoom = new StudyRoom(456L, 1L, "테스트 방", StudyRoomCategory.EMPLOYMENT);

        // when
        String generatedRoomId = studyRoom.generateLiveKitRoomId();

        // then
        assertEquals("study-room-1", generatedRoomId);
    }

    @Test
    @DisplayName("라이브킷 방 ID 미설정 시 hasLiveKitRoom() false 반환")
    void 라이브킷_방ID_미설정_테스트() {
        // given
        StudyRoom studyRoom = new StudyRoom(123L, 1L, "테스트 방", StudyRoomCategory.SELF_STUDY);

        // when & then
        assertFalse(studyRoom.hasLiveKitRoom());
    }

    @Test
    @DisplayName("전체 음소거 토글 확인")
    void 전체_음소거_토글_테스트() {
        // given
        StudyRoom studyRoom = StudyRoom.builder()
                .roomId(10L)
                .userId(1L)
                .title("음소거 테스트 방")
                .category(StudyRoomCategory.SCHOOL_STUDY)
                .build();

        // default
        assertFalse(studyRoom.isAllMuted());

        // when - 전체 음소거 활성화
        studyRoom.enableAllMute();
        assertTrue(studyRoom.isAllMuted());

        // when - 전체 음소거 비활성화
        studyRoom.disableAllMute();
        assertFalse(studyRoom.isAllMuted());
    }

    @Test
    @DisplayName("방 삭제(delete) 시 deletedAt 설정 확인")
    void delete_테스트() {
        // given
        StudyRoom studyRoom = StudyRoom.builder()
                .roomId(20L)
                .userId(2L)
                .title("삭제 테스트 방")
                .category(StudyRoomCategory.LANGUAGE)
                .build();

        assertNull(studyRoom.getDeletedAt());

        // when
        studyRoom.delete();

        // then
        assertNotNull(studyRoom.getDeletedAt());
        assertTrue(studyRoom.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("빌더로 생성 시 기본값 및 필드 확인")
    void 빌더로_생성_테스트() {
        // given & when
        StudyRoom studyRoom = StudyRoom.builder()
                .roomId(30L)
                .userId(3L)
                .title("빌더 테스트 방")
                .description("상세 설명")
                .password("pwd")
                .maxParticipants(4)
                .tag("테스트")
                .category(StudyRoomCategory.CERTIFICATION)
                .focusTime(25)
                .breakTime(5)
                .inviteHashCode("HASH12345")
                .liveKitRoomId("room_hash_test")
                .isAllMuted(true)
                .build();

        // then
        assertEquals(30L, studyRoom.getRoomId());
        assertEquals(3L, studyRoom.getUserId());
        assertEquals("빌더 테스트 방", studyRoom.getTitle());
        assertEquals("상세 설명", studyRoom.getDescription());
        assertEquals("pwd", studyRoom.getPassword());
        assertEquals(4, studyRoom.getMaxParticipants());
        assertEquals("테스트", studyRoom.getTag());
        assertEquals(StudyRoomCategory.CERTIFICATION, studyRoom.getCategory());
        assertEquals(25, studyRoom.getFocusTime());
        assertEquals(5, studyRoom.getBreakTime());
        assertEquals("HASH12345", studyRoom.getInviteHashCode());
        assertEquals("room_hash_test", studyRoom.getLiveKitRoomId());
        assertTrue(studyRoom.hasLiveKitRoom());
        assertTrue(studyRoom.isAllMuted());
    }
}
