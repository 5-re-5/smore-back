package org.oreo.smore.domain.participant;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Slf4j
@Entity
@Table(name= "participants")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted = false;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Builder
    public  Participant(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
        this.isMuted = false;
        this.isBanned = false;

        log.debug("새 참가자 객체 생성 - 방 ID: {}, 사용자ID: {}", roomId, userId);
    }

    // 음소거 설정
    public void mute() {
        this.isMuted = true;
        log.info("참가자 음소거 설정 - 방ID: {}, 사용자ID: {}", roomId, userId);
    }

    // 음소거 해제
    public void unmute() {
        this.isMuted = false;
        log.info("참가자 음소거 해제 - 방ID: {}, 사용자ID: {}", roomId, userId);
    }

    // 강퇴 처리
    public void ban() {
        this.isBanned = true;
        this.leftAt = LocalDateTime.now();
        log.warn("참가자 강퇴 처리 - 방ID: {}, 사용자ID: {}, 강퇴시간: {}",
                roomId, userId, leftAt);
    }

    // 퇴장 처리
    public void leave() {
        this.leftAt = LocalDateTime.now();
        log.info("참가자 퇴장 처리 - 방ID: {}, 사용자ID: {}, 퇴장시간: {}",
                roomId, userId, leftAt);
    }

    // 현재 방에 있는지 확인
    public boolean isInRoom() {
        boolean inRoom = leftAt == null && !isBanned;
        log.debug("참가자 방 존재 여부 확인 - 방ID: {}, 사용자ID: {}, 방 안에 있음: {}",
                roomId, userId, inRoom);
        return inRoom;
    }

    // 강퇴 당했는지 확인
    public boolean isBannedFromRoom() {
        log.debug("참가자 강퇴 상태 확인 - 방ID: {}, 사용자ID: {}, 강퇴됨: {}",
                roomId, userId, isBanned);
        return isBanned;
    }

    // 음소거 상태인지 확인
    public boolean isMutedInRoom() {
        log.debug("참가자 음소거 상태 확인 - 방ID: {}, 사용자ID: {}, 음소거됨: {}",
                roomId, userId, isMuted);
        return isMuted;
    }
}
