package org.oreo.smore.domain.studyroom;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String password;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 6;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column
    private String tag;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyRoomCategory category;

    @Column(name = "focus_time")
    private Integer focusTime;

    @Column(name = "break_time")
    private Integer breakTime;

    @Column(name = "invite_hash_code")
    private String inviteHashCode;

    @Column(name = "invite_created_at")
    private LocalDateTime inviteCreatedAt;

    @Column(name = "openvidu_session_id")
    private String openViduSessionId;

    // soft delete용
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }



    // 테스트용 생성자
    public StudyRoom(Long userId, Long roomId, String title, StudyRoomCategory category) {
        this.userId = userId;
        this.roomId = roomId;
        this.title = title;
        this.category = category;
        this.maxParticipants = 6;
    }
}
