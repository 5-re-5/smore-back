package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
@Rollback
class ChatRoomEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 세션을 정리
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후에 세션을 정리
        entityManager.clear();
    }

    @Test
    @DisplayName("ChatRoom 생성 및 저장 테스트")
    void createAndSaveChatRoom() {
        // given
        StudyRoom studyRoom = StudyRoom.builder()
                .userId(1L)
                .title("테스트 스터디룸")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .build();

        // StudyRoom을 먼저 저장해야 ID가 생성됨
        StudyRoom savedStudyRoom = entityManager.persistAndFlush(studyRoom);

        // ✅ 세션에서 detach 후 다시 조회 (선택사항)
        entityManager.detach(savedStudyRoom);
        StudyRoom foundStudyRoom = entityManager.find(StudyRoom.class, savedStudyRoom.getRoomId());

        ChatRoom chatRoom = ChatRoom.builder()
                .studyRoom(foundStudyRoom) // detach된 객체 사용
                .build();

        // when
        ChatRoom savedChatRoom = entityManager.persistAndFlush(chatRoom);

        // then
        assertThat(savedChatRoom.getStudyRoomId()).isEqualTo(foundStudyRoom.getRoomId());
        assertThat(savedChatRoom.getStudyRoom()).isEqualTo(foundStudyRoom);
        assertThat(savedChatRoom.getTotalMessageCount()).isEqualTo(0L);
        assertThat(savedChatRoom.getIsActive()).isTrue();
        assertThat(savedChatRoom.getLastMessageAt()).isNotNull();
    }

    @Test
    @DisplayName("ChatRoom 메시지 업데이트 테스트")
    void updateLastMessage() {
        // given
        StudyRoom studyRoom = StudyRoom.builder()
                .userId(1L)
                .title("테스트 스터디룸")
                .category(StudyRoomCategory.SELF_STUDY)
                .build();

        StudyRoom savedStudyRoom = entityManager.persistAndFlush(studyRoom);

        ChatRoom chatRoom = ChatRoom.builder()
                .studyRoom(savedStudyRoom)
                .build();

        ChatRoom savedChatRoom = entityManager.persistAndFlush(chatRoom);

        // ✅ 업데이트 전 상태 기록
        LocalDateTime beforeUpdate = savedChatRoom.getLastMessageAt();
        Long beforeCount = savedChatRoom.getTotalMessageCount();

        // ✅ 약간의 시간 차이 확보
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // when
        savedChatRoom.updateLastMessage();
        // ✅ merge 사용 (이미 세션에 있는 엔티티 업데이트)
        ChatRoom updatedChatRoom = entityManager.merge(savedChatRoom);
        entityManager.flush();

        // then
        assertThat(updatedChatRoom.getLastMessageAt()).isAfter(beforeUpdate);
        assertThat(updatedChatRoom.getTotalMessageCount()).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("ChatRoom 비활성화 테스트")
    void deactivateChatRoom() {
        // given
        StudyRoom studyRoom = StudyRoom.builder()
                .userId(1L)
                .title("테스트 스터디룸")
                .category(StudyRoomCategory.SELF_STUDY)
                .build();

        StudyRoom savedStudyRoom = entityManager.persistAndFlush(studyRoom);

        ChatRoom chatRoom = ChatRoom.builder()
                .studyRoom(savedStudyRoom)
                .build();

        ChatRoom savedChatRoom = entityManager.persistAndFlush(chatRoom);

        // when
        savedChatRoom.deactivate();
        // ✅ merge 사용 (이미 세션에 있는 엔티티 업데이트)
        ChatRoom deactivatedChatRoom = entityManager.merge(savedChatRoom);
        entityManager.flush();

        // then
        assertThat(deactivatedChatRoom.getIsActive()).isFalse();
    }
}