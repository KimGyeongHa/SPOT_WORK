package com.sukima.api.infrastructure.persistence.entity.application;

import com.sukima.api.domain.application.type.ApplicationStatus;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "job_applications",
    uniqueConstraints = @UniqueConstraint(columnNames = {"job_posting_id", "worker_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPostingEntity jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerEntity worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public JobApplicationEntity(Long id, JobPostingEntity jobPosting, WorkerEntity worker,
                                ApplicationStatus status, LocalDateTime appliedAt) {
        this.id = id;
        this.jobPosting = jobPosting;
        this.worker = worker;
        this.status = status;
        this.appliedAt = appliedAt;
    }
}
