package org.oreo.smore.domain.focusrecord;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "focus_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FocusRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "`timestamp`", nullable = false)
    private Instant timestamp;

    @Column(name = "status", nullable = false)
    private Integer status;
}
