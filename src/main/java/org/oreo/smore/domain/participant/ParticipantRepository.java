package org.oreo.smore.domain.participant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    // 특정 방의 모든 참가자 조회 (입장 시간 순)
    List<Participant> findByRoomIdOrderByJoinedAtAsc(Long roomId);

    @Query("SELECT p FROM Participant p WHERE p.roomId = :roomId AND p.userId = :userId AND p.leftAt IS NULL AND p.isBanned = false")
    Optional<Participant> findActiveParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 특정 방의 현재 참가자 조회
    @Query("SELECT p FROM Participant p WHERE p.roomId = :roomId AND p.leftAt IS NULL AND p.isBanned = false")
    List<Participant> findActiveParticipantsByRoomId(@Param("roomId") Long roomId);

    // 특정 사용자가 참가한 모든 방 조회 (취근 참가 순)
    List<Participant> findByUserIdOrderByJoinedAtDesc(Long userId);
    
    // 특정 방의 현재 참가자 수 조회
    @Query("SELECT COUNT(p) FROM Participant p WHERE p.roomId = :roomId AND p.leftAt IS NULL AND p.isBanned = false")
    long countActiveParticipantsByRoomId(@Param("roomId") Long roomId);
    
    // 특정 방의 음소거된 참가자 조회
    @Query("SELECT p FROM Participant p WHERE p.roomId = :roomId AND p.leftAt IS NULL AND p.isBanned = false AND p.audioEnabled = false")
    List<Participant> findMutedParticipantsByRoomId(@Param("roomId") Long roomId);

    // 특정 방의 참가 이력 삭제 (방 삭제시 사용)
    void deleteByRoomId(Long roomId);

    // 특정 사용자의 특정 방 참가 이력 삭제
    void deleteByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomIdAndLeftAtIsNull(Long roomId);

    List<Participant> findAllByRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);

    List<Participant> findAllByRoomIdAndLeftAtIsNull(Long roomId);

    // 여러 방의 참가자 수를 한 번의 쿼리로 가져옴
    @Query("SELECT p.roomId, COUNT(p) FROM Participant p " +
            "WHERE p.roomId IN :roomIds " +
            "AND p.leftAt IS NULL " +
            "AND p.isBanned = false " +
            "GROUP BY p.roomId")
    List<Object[]> countActiveParticipantsByRoomIds(@Param("roomIds") List<Long> roomIds);
}
