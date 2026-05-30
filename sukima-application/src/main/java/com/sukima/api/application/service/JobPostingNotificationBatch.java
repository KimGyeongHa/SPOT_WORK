package com.sukima.api.application.service;

import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@Transactional(readOnly = true)
public class JobPostingNotificationBatch {

    private final WorkerJpaRepository workerJpaRepository;
    private final NotificationPort notificationPort;
    private final Executor notificationExecutor;

    public JobPostingNotificationBatch(
            WorkerJpaRepository workerJpaRepository,
            NotificationPort notificationPort,
            @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.workerJpaRepository = workerJpaRepository;
        this.notificationPort = notificationPort;
        this.notificationExecutor = notificationExecutor;
    }

    public void notifyNearbyWorkers(Long jobPostingId, String jobTitle, Point location) {
        double jobLat = location.getY();
        double jobLng = location.getX();

        List<WorkerEntity> workers = workerJpaRepository.findWorkersInRangeOf(jobLat, jobLng);

        if (workers.isEmpty()) return;

        log.info("신규 공고 알림 시작: jobPostingId={}, 대상 {}명", jobPostingId, workers.size());

        // ThreadPoolTaskExecutor로 병렬 처리
        List<CompletableFuture<Void>> futures = workers.stream()
                .map(worker -> CompletableFuture.runAsync(
                        () -> sendNotification(worker, jobPostingId, jobTitle),
                        notificationExecutor
                ))
                .toList();

        // 전체 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("알림 발송 중 오류 발생: jobPostingId={}", jobPostingId, ex);
                    } else {
                        log.info("신규 공고 알림 완료: jobPostingId={}, {}명 발송", jobPostingId, workers.size());
                    }
                });
    }

    private void sendNotification(WorkerEntity worker, Long jobPostingId, String jobTitle) {
        try {
            notificationPort.notifyNewJobPosting(
                    worker.getUser().getId(),
                    jobPostingId,
                    jobTitle
            );
        } catch (Exception e) {
            log.warn("알림 발송 실패: workerId={}, jobPostingId={}", worker.getId(), jobPostingId, e);
        }
    }
}
