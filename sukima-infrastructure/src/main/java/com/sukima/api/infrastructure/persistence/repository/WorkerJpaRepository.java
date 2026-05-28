package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerJpaRepository extends JpaRepository<WorkerEntity, Long> {

    Optional<WorkerEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);

    // 알림 활성화 + 위치 설정된 Worker 전체 조회 (배치용)
    List<WorkerEntity> findAllByNotificationEnabledTrueAndNotificationLatIsNotNull();
}
