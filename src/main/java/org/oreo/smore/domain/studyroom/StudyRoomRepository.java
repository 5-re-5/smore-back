package org.oreo.smore.domain.studyroom;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    Optional<StudyRoom> findByRoomIdAndDeletedAtIsNull(Long roomId);
    // 전체 스터디룸을 최근 생성된 순으로 조회
    List<StudyRoom> findAllByOrderByCreatedAtDesc();

    // 삭제되지 않은 스터디룸을 최근 생성된 순으로 조회
    List<StudyRoom> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    // 특정 카테고리 스터디룸을 최근 생성된 순으로 조회
    List<StudyRoom> findAllByCategoryAndDeletedAtIsNullOrderByCreatedAtDesc(StudyRoomCategory category);

    // cursor 기반 페이징을 위한 메서드
    Slice<StudyRoom> findAll(Specification<StudyRoom> spec, Pageable pageable);

    Optional<StudyRoom> findByLiveKitRoomId(String liveKitRoomId);

    @Query("SELECT DISTINCT sr FROM StudyRoom sr " +
            "LEFT JOIN FETCH sr.user " +
            "WHERE sr.deletedAt IS NULL " +
            "AND sr.roomId < :cursor " +
            "ORDER BY sr.roomId DESC")
    List<StudyRoom> findAllWithUserFetchJoin(
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
