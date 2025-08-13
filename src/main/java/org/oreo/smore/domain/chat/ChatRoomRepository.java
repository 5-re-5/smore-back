package org.oreo.smore.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // StudyRoom ID로 chatroom 조회
    Optional<ChatRoom> findByStudyRoomId(Long studyRoomId);

    // 활성 상태인 ChatRoom 조회
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.studyRoomId = :studyRoomId AND cr.isActive = true")
    Optional<ChatRoom> findActiveByStudyRoomId(@Param("studyRoomId") Long studyRoomId);

    // 비활성 ChatRoom 목록 조회 -> 배치 정리용
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.lastMessageAt < :before AND cr.isActive = true")
    List<ChatRoom> findInactiveRoomsBefore(@Param("before") LocalDateTime before);

    // ChatRoom 메시지 수 업데이트
    @Modifying
    @Query("UPDATE ChatRoom cr SET cr.totalMessageCount = :messageCount, cr.updatedAt = CURRENT_TIMESTAMP WHERE cr.studyRoomId = :studyRoomId")
    void updateMessageCount(@Param("studyRoomId") Long studyRoomId, @Param("messageCount") Long messageCount);

    // studyroom 삭제 시 chatroom 비활성화
    @Modifying
    @Query("UPDATE ChatRoom cr SET cr.isActive = false, cr.updatedAt = CURRENT_TIMESTAMP WHERE cr.studyRoomId = :studyRoomId")
    void deactivateByStudyRoomId(@Param("studyRoomId") Long studyRoomId);

    // 최근 활동이 있는 chatroom 목록 조회
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.lastMessageAt >= :since AND cr.isActive = true ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findActiveRoomsSince(@Param("since") LocalDateTime since);

    // StudyRoom ID 목록으로 ChatRoom 존재 여부 확인
    @Query("SELECT cr.studyRoomId FROM ChatRoom cr WHERE cr.studyRoomId IN :studyRoomIds AND cr.isActive = true")
    List<Long> findExistingStudyRoomIds(@Param("studyRoomIds") List<Long> studyRoomIds);
}
