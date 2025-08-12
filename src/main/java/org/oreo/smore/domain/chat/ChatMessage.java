package org.oreo.smore.domain.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.oreo.smore.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                // 키셋 페이지네이션
                @Index(
                        name = "idx_chat_message_room_pagination",
                        columnList = "room_id, created_at DESC, id DESC"
                ),
                // 소프트 삭제 조회
                @Index(
                        name = "idx_chat_message_room_deleted",
                        columnList = "room_id, deleted_at"
                ),
                // 사용자별 메시지 조회
                @Index(
                        name = "idx_chat_message_user_created",
                        columnList = "user_id, created_at DESC"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // 메시지 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    // 메시지 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 소프트 삭제 -> 30일 후 배치 삭제
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 수정된 메시지 여부
    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    @Column(name = "original_message_id")
    private Long originalMessageId;

    @Builder
    public ChatMessage(Long roomId, User user, String content, MessageType messageType) {
        this.roomId = roomId;
        this.user = user;
        this.content = content;
        this.messageType = messageType;
        this.isEdited = false;
    }

    // 메시지 수정
    public void editMessage(String newContent) {
        this.content = newContent;
        this.isEdited = true;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public static ChatMessage createSystemMessage(Long roomId, String content, MessageType messageType) {
        return ChatMessage.builder()
                .roomId(roomId)
                .user(null) // 시스템 메시지는 user null
                .content(content)
                .messageType(messageType)
                .build();
    }
}
