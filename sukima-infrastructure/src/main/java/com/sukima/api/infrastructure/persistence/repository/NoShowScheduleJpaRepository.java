package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.noshow.NoShowScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoShowScheduleJpaRepository extends JpaRepository<NoShowScheduleEntity, Long> {

    Optional<NoShowScheduleEntity> findByMatchId(Long matchId);

    // 미처리 + triggerAt 아직 안 지난 것 (Redis 재등록 대상)
    List<NoShowScheduleEntity> findAllByProcessedYnAndTriggerAtAfter(String processedYn, LocalDateTime now);

    // 미처리 + triggerAt 이미 지난 것 (서버 다운 중 놓친 것)
    List<NoShowScheduleEntity> findAllByProcessedYnAndTriggerAtBefore(String processedYn, LocalDateTime now);
}
