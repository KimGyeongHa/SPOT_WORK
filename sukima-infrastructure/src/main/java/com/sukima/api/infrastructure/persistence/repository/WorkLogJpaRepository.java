package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.domain.worklog.type.WorkLogType;
import com.sukima.api.infrastructure.persistence.entity.worklog.WorkLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkLogJpaRepository extends JpaRepository<WorkLogEntity, Long> {

    Optional<WorkLogEntity> findByMatchIdAndType(Long matchId, WorkLogType type);
    boolean existsByMatchIdAndType(Long matchId, WorkLogType type);
}
