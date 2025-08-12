package org.oreo.smore.domain.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 키셋 페이지네이션으로 채팅 메시지 조회
    @Query("""
        SELECT cm FROM ChatMessage cm 
        WHERE cm.roomId = :roomId 
        AND cm.deletedAt IS NULL
        AND (:lastMessageId IS NULL OR 
             cm.createdAt < :lastCreatedAt OR 
             (cm.createdAt = :lastCreatedAt AND cm.id < :lastMessageId))
        ORDER BY cm.createdAt DESC, cm.id DESC
        """)
    Slice<ChatMessage> findMessagesByRoomIdWithKeyset(
            @Param("roomId") Long roomId,
            @Param("lastMessageId") Long lastMessageId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            Pageable pageable
    );

    // 특정 시간 이후의 메시지 조회
    @Query("""
        SELECT cm FROM ChatMessage cm 
        WHERE cm.roomId = :roomId 
        AND cm.deletedAt IS NULL
        AND cm.createdAt > :since
        ORDER BY cm.createdAt ASC, cm.id ASC
        """)
    List<ChatMessage> findRecentMessages(
            @Param("roomId") Long roomId,
            @Param("since") LocalDateTime since
    );

    // 채팅방의 최신 메시지 조회
    @Query("""
        SELECT cm FROM ChatMessage cm 
        WHERE cm.roomId = :roomId 
        AND cm.deletedAt IS NULL
        ORDER BY cm.createdAt DESC, cm.id DESC
        LIMIT 1
        """)
    Optional<ChatMessage> findLatestMessageByRoomId(@Param("roomId") Long roomId);

    // 특정 사용자의 메시지 조회
    @Query("""
        SELECT cm FROM ChatMessage cm 
        WHERE cm.roomId = :roomId 
        AND cm.user.userId = :userId
        AND cm.deletedAt IS NULL
        ORDER BY cm.createdAt DESC
        """)
    Slice<ChatMessage> findMessagesByUserAndRoom(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            Pageable pageable
    );

    // 메시지 타입별 조회
    @Query("""
        SELECT cm FROM ChatMessage cm 
        WHERE cm.roomId = :roomId 
        AND cm.messageType = :messageType
        AND cm.deletedAt IS NULL
        ORDER BY cm.createdAt DESC
        """)
    Slice<ChatMessage> findMessagesByTypeAndRoom(
            @Param("roomId") Long roomId,
            @Param("messageType") MessageType messageType,
            Pageable pageable
    );

    // 채팅방의 총 메시지 수 조회
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.deletedAt IS NULL")
    Long countActiveMessagesByRoomId(@Param("roomId") Long roomId);

    // 소프트 삭제 처리
    @Modifying
    @Query("""
        UPDATE ChatMessage cm 
        SET cm.deletedAt = CURRENT_TIMESTAMP 
        WHERE cm.id = :messageId 
        AND cm.roomId = :roomId 
        AND cm.user.userId = :userId
        AND cm.deletedAt IS NULL
        """)
    int softDeleteMessage(
            @Param("messageId") Long messageId,
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    // 30일 지난 메시지 물리 삭제 -> 배치 작업
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.deletedAt IS NOT NULL AND cm.deletedAt < :before")
    int deleteOldSoftDeletedMessages(@Param("before") LocalDateTime before);

    // 채팅방 별 오래된 메시지 삭제
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.createdAt < :before")
    int deleteOldMessagesByRoom(@Param("roomId") Long roomId, @Param("before") LocalDateTime before);

    // chatroom 삭제 시 모든 메시지 삭제
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.deletedAt = CURRENT_TIMESTAMP WHERE cm.roomId = :roomId AND cm.deletedAt IS NULL")
    void softDeleteAllMessagesByRoomId(@Param("roomId") Long roomId);

    // 사용자 별 최근 메시지 활동 조회
    @Query("""
        SELECT COUNT(cm) FROM ChatMessage cm 
        WHERE cm.user.userId = :userId 
        AND cm.deletedAt IS NULL
        AND cm.createdAt >= :since
        """)
    Long countUserMessagesAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
