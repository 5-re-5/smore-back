package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChatRoom 엔티티 매핑 테스트")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatRoomEntityTest {

    @Autowired
    private TestEntityManager entityManager;

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

        ChatRoom chatRoom = ChatRoom.builder()
                .studyRoom(savedStudyRoom)
                .build();

        // when
        ChatRoom savedChatRoom = entityManager.persistAndFlush(chatRoom);

        // then
        assertThat(savedChatRoom.getStudyRoomId()).isEqualTo(savedStudyRoom.getRoomId());
        assertThat(savedChatRoom.getStudyRoom()).isEqualTo(savedStudyRoom);
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

        LocalDateTime beforeUpdate = savedChatRoom.getLastMessageAt();
        Long beforeCount = savedChatRoom.getTotalMessageCount();

        // when
        savedChatRoom.updateLastMessage();
        entityManager.persistAndFlush(savedChatRoom);

        // then
        assertThat(savedChatRoom.getLastMessageAt()).isAfter(beforeUpdate);
        assertThat(savedChatRoom.getTotalMessageCount()).isEqualTo(beforeCount + 1);
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
        entityManager.persistAndFlush(savedChatRoom);

        // then
        assertThat(savedChatRoom.getIsActive()).isFalse();
    }
}
