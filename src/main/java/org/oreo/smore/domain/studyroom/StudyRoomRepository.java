package org.oreo.smore.domain.studyroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    // 전체 스터디룸을 최근 생성된 순으로 조회
    List<StudyRoom> findAllByOrderByCreatedAtDesc();

    // 삭제되지 않은 스터디룸을 최근 생성된 순으로 조회
    List<StudyRoom> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    // 특정 카테고리 스터디룸을 최근 생성된 순으로 조회
    List<StudyRoom> findAllByCategoryAndDeletedAtIsNullOrderByCreatedAtDesc(StudyRoomCategory category);
}
