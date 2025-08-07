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

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Column(name = "audio_enabled", nullable = false)
    private Boolean audioEnabled = true; // 기본값: 마이크 켜짐

    @Column(name = "video_enabled", nullable = false)
    private Boolean videoEnabled = true; // 기본값: 카메라 켜짐

    @Builder
    public  Participant(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
        this.isBanned = false;
        this.audioEnabled = true;
        this.videoEnabled = true;

        log.debug("새 참가자 객체 생성 - 방 ID: {}, 사용자ID: {}", roomId, userId);
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
            log.debug("@PrePersist - joinedAt 설정: {}", joinedAt);
        }
    }

    // 오디오 활성화
    public void enableAudio(String controllerType) {
        this.audioEnabled = true;
        log.info("참가자 오디오 활성화 - 방ID: {}, 사용자ID: {}, 제어자: {}",
                roomId, userId, controllerType);
    }

    // 오디오 비활성화
    public void disableAudio(String controllerType) {
        this.audioEnabled = false;
        log.info("참가자 오디오 비활성화 - 방ID: {}, 사용자ID: {}, 제어자: {}",
                roomId, userId, controllerType);
    }

    // 비디오 활성화
    public void enableVideo(String controllerType) {
        this.videoEnabled = true;
        log.info("참가자 비디오 활성화 - 방ID: {}, 사용자ID: {}, 제어자: {}",
                roomId, userId, controllerType);
    }

    // 비디오 비활성화
    public void disableVideo(String controllerType) {
        this.videoEnabled = false;
        log.info("참가자 비디오 비활성화 - 방ID: {}, 사용자ID: {}, 제어자: {}",
                roomId, userId, controllerType);
    }

    // 미디어 상태 일괄 업데이트
    public void updateMediaStatus(Boolean audioEnabled, Boolean videoEnabled, String controllerType) {
        if (audioEnabled != null && !this.audioEnabled.equals(audioEnabled)) {
            this.audioEnabled = audioEnabled;
            log.info("참가자 오디오 상태 변경 - 방ID: {}, 사용자ID: {}, 오디오: {}, 제어자: {}",
                    roomId, userId, audioEnabled, controllerType);
        }
        if (videoEnabled != null && !this.videoEnabled.equals(videoEnabled)) {
            this.videoEnabled = videoEnabled;
            log.info("참가자 비디오 상태 변경 - 방ID: {}, 사용자ID: {}, 비디오: {}, 제어자: {}",
                    roomId, userId, videoEnabled, controllerType);
        }
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

    // 음소거 설정 (방장용 - 기존 호환성)
    public void mute() {
        disableAudio("방장");
    }

    // 음소거 해제 (방장용 - 기존 호환성)
    public void unmute() {
        enableAudio("방장");
    }

    public boolean isMutedInRoom() {
        boolean muted = !audioEnabled;
        log.debug("참가자 음소거 상태 확인 - 방ID: {}, 사용자ID: {}, 음소거됨: {}",
                roomId, userId, muted);
        return muted;
    }

    // 🔥 새로운 상태 확인 메서드들
    public boolean isAudioEnabled() {
        return audioEnabled;
    }

    public boolean isVideoEnabled() {
        return videoEnabled;
    }
}
