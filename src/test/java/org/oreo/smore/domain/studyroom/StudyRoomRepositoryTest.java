package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class StudyRoomRepositoryTest {

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Test
    @DisplayName("삭제되지 않은 스터디룸을 최근 생성된 순으로 조회할 수 있다")
    void 삭제되지않은_스터디룸_최신순_조회() {
        // given
        StudyRoom room1 = new StudyRoom(1L, "첫 번째 스터디", StudyRoomCategory.EMPLOYMENT);
        StudyRoom room2 = new StudyRoom(2L, "두 번째 스터디", StudyRoomCategory.CERTIFICATION);
        StudyRoom room3 = new StudyRoom(3L, "세 번째 스터디", StudyRoomCategory.EMPLOYMENT);

        studyRoomRepository.save(room1);
        studyRoomRepository.save(room2);
        studyRoomRepository.save(room3);

        // when
        List<StudyRoom> rooms = studyRoomRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();

        // then
        assertThat(rooms).hasSize(3);
        assertThat(rooms).extracting(StudyRoom::getTitle)
                .containsExactlyInAnyOrder("첫 번째 스터디", "두 번째 스터디", "세 번째 스터디");
        assertThat(rooms).allMatch(room -> room.getDeletedAt() == null);
    }

    @Test
    @DisplayName("특정 카테고리의 스터디룸을 조회할 수 있다")
    void 카테고리별_스터디룸_조회() {
        // given
        StudyRoom employmentRoom1 = new StudyRoom(1L, "취업 스터디 1", StudyRoomCategory.EMPLOYMENT);
        StudyRoom employmentRoom2 = new StudyRoom(2L, "취업 스터디 2", StudyRoomCategory.EMPLOYMENT);
        StudyRoom certificationRoom = new StudyRoom(3L, "자격증 스터디", StudyRoomCategory.CERTIFICATION);

        studyRoomRepository.save(employmentRoom1);
        studyRoomRepository.save(employmentRoom2);
        studyRoomRepository.save(certificationRoom);

        // when
        List<StudyRoom> employmentRooms = studyRoomRepository
                .findAllByCategoryAndDeletedAtIsNullOrderByCreatedAtDesc(StudyRoomCategory.EMPLOYMENT);

        // then
        assertThat(employmentRooms).hasSize(2);
        assertThat(employmentRooms).allMatch(room -> room.getCategory() == StudyRoomCategory.EMPLOYMENT);
        assertThat(employmentRooms).extracting(StudyRoom::getTitle)
                .containsExactlyInAnyOrder("취업 스터디 1", "취업 스터디 2");
    }

    @Test
    @DisplayName("삭제된 스터디룸은 조회되지 않는다")
    void 삭제된_스터디룸_제외_조회() {
        // given
        StudyRoom activeRoom = new StudyRoom(1L, "활성 스터디", StudyRoomCategory.EMPLOYMENT);
        StudyRoom roomToDelete = new StudyRoom(2L, "삭제될 스터디", StudyRoomCategory.EMPLOYMENT);

        studyRoomRepository.save(activeRoom);
        studyRoomRepository.save(roomToDelete);

        // 스터디룸 삭제
        roomToDelete.delete();
        studyRoomRepository.save(roomToDelete);

        // when
        List<StudyRoom> activeRooms = studyRoomRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();

        // then
        assertThat(activeRooms).hasSize(1);
        assertThat(activeRooms.get(0).getTitle()).isEqualTo("활성 스터디");
    }

    @Test
    @DisplayName("OpenVidu 세션을 할당하고 해제할 수 있다")
    void openVidu세션_할당_및_해제() {
        // given
        StudyRoom studyRoom = new StudyRoom(1L, "비디오 스터디", StudyRoomCategory.EMPLOYMENT);
        StudyRoom savedRoom = studyRoomRepository.save(studyRoom);

        // when - 세션 할당
        savedRoom.assignOpenViduSession("ses_abc123");
        studyRoomRepository.save(savedRoom);

        // then - 세션 할당 확인
        StudyRoom roomWithSession = studyRoomRepository.findById(savedRoom.getRoomId()).orElseThrow();
        assertThat(roomWithSession.getOpenViduSessionId()).isEqualTo("ses_abc123");

        // when - 세션 해제
        roomWithSession.clearOpenViduSession();
        studyRoomRepository.save(roomWithSession);

        // then - 세션 해제 확인
        StudyRoom roomWithoutSession = studyRoomRepository.findById(savedRoom.getRoomId()).orElseThrow();
        assertThat(roomWithoutSession.getOpenViduSessionId()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 조회하면 빈 리스트를 반환한다")
    void 존재하지않는_카테고리_조회() {
        // given
        StudyRoom room = new StudyRoom(1L, "스터디", StudyRoomCategory.EMPLOYMENT);
        studyRoomRepository.save(room);

        // when
        List<StudyRoom> rooms = studyRoomRepository
                .findAllByCategoryAndDeletedAtIsNullOrderByCreatedAtDesc(StudyRoomCategory.MEETING);

        // then
        assertThat(rooms).isEmpty();
    }
}
