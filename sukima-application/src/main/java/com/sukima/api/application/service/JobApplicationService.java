package com.sukima.api.application.service;

import com.sukima.api.application.port.in.application.AcceptApplicationUseCase;
import com.sukima.api.application.port.in.application.ApplyJobUseCase;
import com.sukima.api.domain.application.type.ApplicationStatus;
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

    @Override
    @Transactional
    public Long apply(ApplyJobUseCase.Command command) {
        if (jobApplicationJpaRepository.existsByJobPostingIdAndWorkerId(
                command.jobPostingId(), command.workerId())) {
            throw new IllegalArgumentException("이미 지원한 공고입니다.");
        }

        JobPostingEntity jobPosting = jobPostingJpaRepository.findById(command.jobPostingId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));

        WorkerEntity worker = workerJpaRepository.findById(command.workerId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구직자입니다."));

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원입니다."));

        int acceptedCount = jobApplicationJpaRepository.countByJobPostingIdAndStatus(
                application.getJobPosting().getId(), ApplicationStatus.ACCEPTED.name());

        if (acceptedCount >= application.getJobPosting().getCapacity()) {
            throw new IllegalStateException("정원이 초과되었습니다.");
        }

        MatchEntity match = MatchEntity.builder()
                .application(application)
                .jobPosting(application.getJobPosting())
                .worker(application.getWorker())
                .status(MatchStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build();

        return matchJpaRepository.save(match).getId();
    }
}
