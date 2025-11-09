package org.oreo.smore.domain.studyroom;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.oreo.smore.domain.chat.ChatRoom;
import org.oreo.smore.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_rooms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

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

    // Livekit 에서는 Room Name으로 사용됩 !
    @Column(name = "livekit_room_id")
    private String liveKitRoomId;

    @Column(name = "is_all_muted", nullable = false)
    private Boolean isAllMuted = false; // 기본값: 전체 음소거 비활성화

    @OneToOne(mappedBy = "studyRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ChatRoom chatRoom;

    // soft delete용
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // LiveKit 방 ID 설정
    public void setLiveKitRoomId(String liveKitRoomId) {
        this.liveKitRoomId = liveKitRoomId;
    }

    // LiveKit 방 ID 생성 ("study-room-" + roomId)
    public String generateLiveKitRoomId() {
        return "study-room-" + this.roomId;
    }

    public boolean hasLiveKitRoom() {
        return liveKitRoomId != null && !liveKitRoomId.trim().isEmpty();
    }

    // 전체 음소거 활성화
    public void enableAllMute() {
        this.isAllMuted = true;
    }

    // 전체 음소거 비활성화
    public void disableAllMute() {
        this.isAllMuted = false;
    }

    // 전체 음소거 상태 확인
    public boolean isAllMuted() {
        return isAllMuted != null ? isAllMuted : false;
    }

    // 테스트용 생성자
    public StudyRoom(Long userId, Long roomId, String title, StudyRoomCategory category) {
        this.userId = userId;
        this.roomId = roomId;
        this.title = title;
        this.category = category;
        this.maxParticipants = 6;
    }

    @Builder
    public StudyRoom(Long roomId, Long userId, String title, String description, String password,
                     Integer maxParticipants, String thumbnailUrl, String tag,
                     StudyRoomCategory category, Integer focusTime, Integer breakTime,
                     String inviteHashCode, String liveKitRoomId, Boolean isAllMuted) {
        this.roomId = roomId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.password = password;
        this.maxParticipants = maxParticipants != null ? maxParticipants : 6;
        this.thumbnailUrl = thumbnailUrl;
        this.tag = tag;
        this.category = category;
        this.focusTime = focusTime;
        this.breakTime = breakTime;
        this.inviteHashCode = inviteHashCode;
        this.liveKitRoomId = liveKitRoomId;
        this.isAllMuted = isAllMuted != null ? isAllMuted : false;
        this.initializeChatRoom();
    }

    // chatroom 관련 메서드
    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public boolean hasChatRoom() {
        return this.chatRoom != null;
    }

    public boolean isChatRoomActive() {
        return this.chatRoom != null && this.chatRoom.getIsActive();
    }


    private void initializeChatRoom() {
        if (this.chatRoom == null) {
            this.chatRoom = ChatRoom.builder()
                    .studyRoom(this)
                    .build();
        }
    }
}
