package com.sukima.api.infrastructure.persistence.entity.job;

import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.infrastructure.persistence.base.BaseTimeEntity;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPostingEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private EmployerEntity employer;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)", nullable = false)
    private Point location;

    @Column(nullable = false)
    private String address;

    @Column(name = "hourly_wage", nullable = false)
    private int hourlyWage;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Builder
    public JobPostingEntity(Long id, EmployerEntity employer, String title, String description,
                            Point location, String address, int hourlyWage, int capacity,
                            LocalDateTime startAt, LocalDateTime endAt, JobStatus status) {
        this.id = id;
        this.employer = employer;
        this.title = title;
        this.description = description;
        this.location = location;
        this.address = address;
        this.hourlyWage = hourlyWage;
        this.capacity = capacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }
}
