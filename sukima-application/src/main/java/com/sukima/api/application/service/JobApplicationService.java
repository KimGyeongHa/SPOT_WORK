package com.sukima.api.application.service;

import com.sukima.api.application.port.in.application.AcceptApplicationUseCase;
import com.sukima.api.application.port.in.application.ApplyJobUseCase;
import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
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

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobApplicationService implements ApplyJobUseCase, AcceptApplicationUseCase {

    private final JobApplicationJpaRepository jobApplicationJpaRepository;
    private final JobPostingJpaRepository jobPostingJpaRepository;
    private final WorkerJpaRepository workerJpaRepository;
    private final MatchJpaRepository matchJpaRepository;
    private final NoShowSchedulerPort noShowSchedulerPort;

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
    @Transactional
    public Long accept(AcceptApplicationUseCase.Command command) {
        JobApplicationEntity application = jobApplicationJpaRepository.findById(command.applicationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

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

        // 노쇼 체크 예약 (근무 시작 + 15분 후 Redis TTL 만료 → 이벤트 발행)
        noShowSchedulerPort.schedule(saved.getId(), application.getJobPosting().getStartAt());

        return saved.getId();
    }
}
