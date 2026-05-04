package com.sukima.api.infrastructure.persistence.entity.match;

import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.infrastructure.persistence.entity.application.JobApplicationEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private JobApplicationEntity application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private JobPostingEntity jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private WorkerEntity worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    // QR 토큰 버전 (재발급 시 증가, 이전 토큰 무효화 용도)
    @Column(name = "qr_version", nullable = false)
    private Long qrVersion = 0L;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public MatchEntity(Long id, JobApplicationEntity application, JobPostingEntity jobPosting,
                       WorkerEntity worker, MatchStatus status,
                       LocalDateTime confirmedAt, LocalDateTime completedAt) {
        this.id = id;
        this.application = application;
        this.jobPosting = jobPosting;
        this.worker = worker;
        this.status = status;
        this.qrVersion = 0L;
        this.confirmedAt = confirmedAt;
        this.completedAt = completedAt;
    }

    public void cancelByNoShow() {
        this.status = MatchStatus.CANCELLED;
    }

    public void complete() {
        this.status = MatchStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * QR 토큰 재발급 시 버전 증가 → 이전 토큰 무효화
     */
    public void increaseQrVersion() {
        this.qrVersion++;
    }
}
