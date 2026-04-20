package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchJpaRepository extends JpaRepository<MatchEntity, Long> {

    Optional<MatchEntity> findByApplicationId(Long applicationId);
}
