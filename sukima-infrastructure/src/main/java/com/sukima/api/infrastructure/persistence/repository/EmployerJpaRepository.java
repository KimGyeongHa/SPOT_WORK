package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerJpaRepository extends JpaRepository<EmployerEntity, Long> {

    Optional<EmployerEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
