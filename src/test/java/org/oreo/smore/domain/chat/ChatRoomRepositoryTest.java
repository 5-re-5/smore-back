package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.chat.ChatRoom;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * ChatRoomRepository 테스트
 */
@DataJpaTest
@DisplayName("ChatRoomRepository 테스트")
class ChatRoomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private StudyRoom testStudyRoom;
    private ChatRoom testChatRoom;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testStudyRoom = StudyRoom.builder()
                .userId(1L)
                .title("테스트 스터디룸")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        entityManager.persistAndFlush(testStudyRoom);

        testChatRoom = ChatRoom.builder()
                .studyRoom(testStudyRoom)
                .build();

        entityManager.persistAndFlush(testChatRoom);
    }

    @Test
    @DisplayName("StudyRoom ID로 ChatRoom 조회 테스트")
    void findByStudyRoomId() {
        // when
        Optional<ChatRoom> found = chatRoomRepository.findByStudyRoomId(testStudyRoom.getRoomId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStudyRoomId()).isEqualTo(testStudyRoom.getRoomId());
        assertThat(found.get().getStudyRoom().getTitle()).isEqualTo("테스트 스터디룸");
    }

    @Test
    @DisplayName("활성 상태 ChatRoom 조회 테스트")
    void findActiveByStudyRoomId() {
        // when
        Optional<ChatRoom> found = chatRoomRepository.findActiveByStudyRoomId(testStudyRoom.getRoomId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("비활성 ChatRoom은 조회되지 않는 테스트")
    void findActiveByStudyRoomId_InactiveRoom() {
        // given
        testChatRoom.deactivate();
        entityManager.persistAndFlush(testChatRoom);

        // when
        Optional<ChatRoom> found = chatRoomRepository.findActiveByStudyRoomId(testStudyRoom.getRoomId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("최근 활동 ChatRoom 목록 조회 테스트")
    void findActiveRoomsSince() {
        // given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // when
        List<ChatRoom> recentRooms = chatRoomRepository.findActiveRoomsSince(oneHourAgo);

        // then
        assertThat(recentRooms).hasSize(1);
        assertThat(recentRooms.get(0).getStudyRoomId()).isEqualTo(testStudyRoom.getRoomId());
    }

    @Test
    @DisplayName("ChatRoom 메시지 수 업데이트 테스트")
    void updateMessageCount() {
        // given
        Long newMessageCount = 10L;

        // when
        chatRoomRepository.updateMessageCount(testStudyRoom.getRoomId(), newMessageCount);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<ChatRoom> updated = chatRoomRepository.findByStudyRoomId(testStudyRoom.getRoomId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getTotalMessageCount()).isEqualTo(newMessageCount);
    }

    @Test
    @DisplayName("ChatRoom 비활성화 테스트")
    void deactivateByStudyRoomId() {
        // when
        chatRoomRepository.deactivateByStudyRoomId(testStudyRoom.getRoomId());
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<ChatRoom> updated = chatRoomRepository.findByStudyRoomId(testStudyRoom.getRoomId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getIsActive()).isFalse();
    }
}