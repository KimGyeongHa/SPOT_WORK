package com.sukima.api.application.service;

import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공고 등록 시점에 반경 내 Worker에게 알림 발송.
 * 배치 대신 이벤트 기반으로 처리하여 즉시 알림 + DB 쿼리 최소화.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobPostingNotificationBatch {

    private final WorkerJpaRepository workerJpaRepository;
    private final NotificationPort notificationPort;

    public void notifyNearbyWorkers(Long jobPostingId, String jobTitle, Point location) {
        double jobLat = location.getY();
        double jobLng = location.getX();

        // 공고 위치 기준 반경 내 알림 설정된 Worker 단 1번 쿼리
        List<WorkerEntity> workers = workerJpaRepository.findWorkersInRangeOf(jobLat, jobLng);

        if (workers.isEmpty()) return;

        log.info("신규 공고 알림: jobPostingId={}, 대상 worker {}명", jobPostingId, workers.size());

        for (WorkerEntity worker : workers) {
            notificationPort.notifyNewJobPosting(
                    worker.getUser().getId(),
                    jobPostingId,
                    jobTitle
            );
        }
    }
}
