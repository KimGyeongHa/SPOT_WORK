package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerJpaRepository extends JpaRepository<WorkerEntity, Long> {

    Optional<WorkerEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
