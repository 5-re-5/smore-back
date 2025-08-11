package org.oreo.smore.domain.point;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "delta", nullable = false)
    private Integer delta;

    @Column(name = "reason", length = 100)
    private String reason;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
