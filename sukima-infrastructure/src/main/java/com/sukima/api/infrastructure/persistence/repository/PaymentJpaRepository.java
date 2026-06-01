package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.payment.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    boolean existsByMatchId(Long matchId);

    Optional<PaymentEntity> findByMatchId(Long matchId);
}
