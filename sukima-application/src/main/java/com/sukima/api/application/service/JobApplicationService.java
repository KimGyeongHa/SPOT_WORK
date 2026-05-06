package com.sukima.api.application.service;

import com.sukima.api.application.port.in.application.AcceptApplicationUseCase;
import com.sukima.api.application.port.in.application.ApplyJobUseCase;
import com.sukima.api.application.port.out.lock.DistributedLockPort;
import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.domain.application.type.ApplicationStatus;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.infrastructure.persistence.entity.application.JobApplicationEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.JobApplicationJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.JobPostingJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobApplicationService implements ApplyJobUseCase, AcceptApplicationUseCase {

    private final JobApplicationJpaRepository jobApplicationJpaRepository;
    private final JobPostingJpaRepository jobPostingJpaRepository;
    private final WorkerJpaRepository workerJpaRepository;
    private final MatchJpaRepository matchJpaRepository;
    private final NoShowSchedulerPort noShowSchedulerPort;
    private final DistributedLockPort distributedLockPort;
    private final NotificationPort notificationPort;

    // 락 키 prefix
    private static final String ACCEPT_LOCK_PREFIX = "lock:accept:job:";

    // 락 대기 시간: 3초
    private static final long LOCK_WAIT_TIME = 3L;

    // 락 유지 시간: 5초 (작업 완료 전 자동 해제 방지)
    private static final long LOCK_LEASE_TIME = 5L;

    @Override
    @Transactional
    public Long apply(ApplyJobUseCase.Command command) {
        WorkerEntity worker = workerJpaRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKER_NOT_FOUND));

        // 패널티 체크
        if (worker.isPenalized()) {
            throw new BusinessException(ErrorCode.WORKER_PENALIZED);
        }

        // 중복지원 체크
        if (jobApplicationJpaRepository.existsByJobPostingIdAndWorkerId(
                command.jobPostingId(), worker.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }

        JobPostingEntity jobPosting = jobPostingJpaRepository.findById(command.jobPostingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));

        JobApplicationEntity entity = JobApplicationEntity.builder()
                .jobPosting(jobPosting)
                .worker(worker)
                .status(ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();

        return jobApplicationJpaRepository.save(entity).getId();
    }

    @Override
    public Long accept(AcceptApplicationUseCase.Command command) {
        // 지원 먼저 조회해서 jobPostingId 획득
        JobApplicationEntity application = jobApplicationJpaRepository.findById(command.applicationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        Long jobPostingId = application.getJobPosting().getId();
        String lockKey = ACCEPT_LOCK_PREFIX + jobPostingId;

        // 같은 공고에 대한 수락 요청을 직렬화
        return distributedLockPort.executeWithLock(
                lockKey,
                LOCK_WAIT_TIME,
                LOCK_LEASE_TIME,
                TimeUnit.SECONDS,
                () -> acceptWithLock(application)
        );
    }

    @Transactional
    public Long acceptWithLock(JobApplicationEntity application) {
        // 락 안에서 정원 체크 (정확한 카운트 보장)
        int acceptedCount = jobApplicationJpaRepository.countByJobPostingIdAndStatus(
                application.getJobPosting().getId(), ApplicationStatus.ACCEPTED.name());

        if (acceptedCount >= application.getJobPosting().getCapacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED);
        }

        MatchEntity match = MatchEntity.builder()
                .application(application)
                .jobPosting(application.getJobPosting())
                .worker(application.getWorker())
                .status(MatchStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build();

        MatchEntity saved = matchJpaRepository.save(match);

        // 노쇼 체크 예약
        noShowSchedulerPort.schedule(saved.getId(), application.getJobPosting().getStartAt());

        // Worker에게 매칭 확정 알림 (SSE)
        notificationPort.notifyMatchConfirmed(
                application.getWorker().getUser().getId(),
                saved.getId(),
                application.getJobPosting().getTitle()
        );

        return saved.getId();
    }
}
