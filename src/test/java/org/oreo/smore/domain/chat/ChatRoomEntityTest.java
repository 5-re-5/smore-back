package org.oreo.smore.domain.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ChatRoom 엔티티 매핑 테스트")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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

        // 세션을 정리하여 detached 상태로 만들기
        entityManager.clear();

        // StudyRoom을 다시 로드
        StudyRoom reloadedStudyRoom = entityManager.find(StudyRoom.class, savedStudyRoom.getRoomId());

        ChatRoom chatRoom = ChatRoom.builder()
                .studyRoom(reloadedStudyRoom)
                .build();

        // when
        ChatRoom savedChatRoom = entityManager.merge(chatRoom);

        // then
        assertThat(savedChatRoom.getStudyRoomId()).isEqualTo(reloadedStudyRoom.getRoomId());
        assertThat(savedChatRoom.getStudyRoom()).isEqualTo(reloadedStudyRoom);
        assertThat(savedChatRoom.getTotalMessageCount()).isEqualTo(0L);
        assertThat(savedChatRoom.getIsActive()).isTrue();
        assertThat(savedChatRoom.getLastMessageAt()).isNotNull();
    }

//    @Test
//    @DisplayName("ChatRoom 메시지 업데이트 테스트")
//    void updateLastMessage() {
//        // given
//        StudyRoom studyRoom = StudyRoom.builder()
//                .userId(1L)
//                .title("테스트 스터디룸")
//                .category(StudyRoomCategory.SELF_STUDY)
//                .build();
//
//        StudyRoom savedStudyRoom = entityManager.persistAndFlush(studyRoom);
//
//        ChatRoom chatRoom = ChatRoom.builder()
//                .studyRoom(savedStudyRoom)
//                .build();
//
//        ChatRoom savedChatRoom = entityManager.merge(chatRoom);
//
//        LocalDateTime beforeUpdate = savedChatRoom.getLastMessageAt();
//        Long beforeCount = savedChatRoom.getTotalMessageCount();
//
//        // when
//        savedChatRoom.updateLastMessage();
//        entityManager.flush(); // 영속화된 엔티티는 flush만 호출
//
//        // then
//        assertThat(savedChatRoom.getLastMessageAt()).isAfter(beforeUpdate);
//        assertThat(savedChatRoom.getTotalMessageCount()).isEqualTo(beforeCount + 1);
//    }

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

        ChatRoom savedChatRoom = entityManager.merge(chatRoom);

        // when
        savedChatRoom.deactivate();
        entityManager.flush(); // 영속화된 엔티티는 flush만 호출

        // then
        assertThat(savedChatRoom.getIsActive()).isFalse();
    }
}