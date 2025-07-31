package org.oreo.smore.domain.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String nickname;

    @Column(name = "profile_url", length = 255)
    private String profileUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "goal_study_time", nullable = false)
    private Integer goalStudyTime;

    @Column(name = "level", length = 255, nullable = false)
    private String level;

    @Column(name = "target_date_title", length = 255)
    private String targetDateTitle;

    @Column(name = "target_date")
    private LocalDateTime targetDate;

    @Column(name = "determination", length = 255)
    private String determination;
}
