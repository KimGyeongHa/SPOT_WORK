package com.sukima.api.application.service;

import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.JobPostingJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 반경 내 신규 공고 알림 배치.
 * 10분마다 알림 활성화된 Worker의 위치 기준으로 신규 OPEN 공고를 조회하여 알림 발송.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostingNotificationBatch {

    private final WorkerJpaRepository workerJpaRepository;
    private final JobPostingJpaRepository jobPostingJpaRepository;
    private final NotificationPort notificationPort;

    // 10분마다 실행
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    @Transactional(readOnly = true)
    public void notifyNewJobPostings() {
        // 배치 실행 시점 기준 10분 전 이후 등록된 공고 조회
        LocalDateTime since = LocalDateTime.now().minusMinutes(10);

        // 알림 활성화 + 위치 설정된 Worker 전체 조회
        List<WorkerEntity> workers = workerJpaRepository
                .findAllByNotificationEnabledTrueAndNotificationLatIsNotNull();

        if (workers.isEmpty()) return;

        log.info("신규 공고 알림 배치 실행: worker {}명", workers.size());

        for (WorkerEntity worker : workers) {
            try {
                notifyWorker(worker, since);
            } catch (Exception e) {
                log.error("Worker 알림 발송 실패: workerId={}", worker.getId(), e);
            }
        }
    }

    private void notifyWorker(WorkerEntity worker, LocalDateTime since) {
        List<JobPostingEntity> newPostings = jobPostingJpaRepository.findNearbyCreatedAfter(
                worker.getNotificationLat(),
                worker.getNotificationLng(),
                worker.getNotificationRadiusMeters(),
                since
        );

        if (newPostings.isEmpty()) return;

        log.info("신규 공고 알림: workerId={}, 공고 {}건", worker.getId(), newPostings.size());

        for (JobPostingEntity posting : newPostings) {
            notificationPort.notifyNewJobPosting(
                    worker.getUser().getId(),
                    posting.getId(),
                    posting.getTitle()
            );
        }
    }
}
