package org.oreo.smore.domain.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.oreo.smore.domain.studyroom.StudyRoom;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {
    // studyroom과 1:1로 동시 생성

    @Id
    @Column(name = "study_room_id")
    private Long studyRoomId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "study_room_id")
    private StudyRoom studyRoom;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "total_message_count", nullable = false)
    private Long totalMessageCount = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ChatRoom(StudyRoom studyRoom) {
        this.studyRoom = studyRoom;
        this.studyRoomId = studyRoom.getRoomId();
        this.lastMessageAt = LocalDateTime.now();
        this.totalMessageCount = 0L;
        this.isActive = true;
    }

    public void updateLastMessage() {
        this.lastMessageAt = LocalDateTime.now();
        this.totalMessageCount++;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void resetMessageCount(Long count) {
        this.totalMessageCount = count;
    }
}
