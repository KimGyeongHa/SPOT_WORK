package com.sukima.api.infrastructure.persistence.entity.worklog;

import com.sukima.api.domain.worklog.type.WorkLogType;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "work_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private MatchEntity match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkLogType type;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)", nullable = false)
    private Point location;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @Builder
    public WorkLogEntity(Long id, MatchEntity match, WorkLogType type,
                         Point location, LocalDateTime scannedAt) {
        this.id = id;
        this.match = match;
        this.type = type;
        this.location = location;
        this.scannedAt = scannedAt;
    }
}
