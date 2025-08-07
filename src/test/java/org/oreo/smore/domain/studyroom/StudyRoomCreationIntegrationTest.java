package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class StudyRoomRepositoryTest {

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void DB_저장_및_조회_테스트() {
        System.out.println("🔥 Repository DB 테스트 시작!");

        // Given - 스터디룸 엔티티 생성
        StudyRoom studyRoom = StudyRoom.builder()
                .userId(1L)
                .title("Repository_테스트_스터디룸")
                .description("가장 간단한 DB 테스트")
                .category(StudyRoomCategory.SCHOOL_STUDY)
                .maxParticipants(4)
                .focusTime(25)
                .breakTime(5)
                .inviteHashCode("TEST123ABCD")
                .liveKitRoomId("study-room-test")
                .build();

        // When - DB 저장
        StudyRoom saved = studyRoomRepository.save(studyRoom);
        entityManager.flush(); // 즉시 DB 반영
        entityManager.clear(); // 영속성 컨텍스트 초기화

        // Then - DB 조회로 검증
        StudyRoom found = studyRoomRepository.findById(saved.getRoomId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getRoomId()).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Repository_테스트_스터디룸");
        assertThat(found.getUserId()).isEqualTo(1L);
        assertThat(found.getFocusTime()).isEqualTo(25);
        assertThat(found.getBreakTime()).isEqualTo(5);
        assertThat(found.getMaxParticipants()).isEqualTo(4);
        assertThat(found.getCategory()).isEqualTo(StudyRoomCategory.SCHOOL_STUDY);
        assertThat(found.getInviteHashCode()).isEqualTo("TEST123ABCD");

        System.out.println("✅ Repository DB 테스트 성공!");
        System.out.println("📍 생성된 방 ID: " + found.getRoomId());
        System.out.println("🏷️ 제목: " + found.getTitle());
        System.out.println("💾 DB 저장/조회 정상 작동!");
    }

    @Test
    void 여러_스터디룸_저장_테스트() {
        // Given - 여러 스터디룸 생성
        StudyRoom room1 = StudyRoom.builder()
                .userId(1L)
                .title("스터디룸1")
                .category(StudyRoomCategory.CERTIFICATION)
                .maxParticipants(2)
                .inviteHashCode("ROOM1ABC")
                .liveKitRoomId("study-room-1")
                .build();

        StudyRoom room2 = StudyRoom.builder()
                .userId(2L)
                .title("스터디룸2")
                .category(StudyRoomCategory.EMPLOYMENT)
                .maxParticipants(6)
                .inviteHashCode("ROOM2DEF")
                .liveKitRoomId("study-room-2")
                .build();

        // When - 저장
        studyRoomRepository.save(room1);
        studyRoomRepository.save(room2);

        // Then - 전체 조회
        Iterable<StudyRoom> allRooms = studyRoomRepository.findAll();
        assertThat(allRooms).hasSize(2);

        System.out.println("✅ 다중 스터디룸 저장 테스트 성공!");
    }
}