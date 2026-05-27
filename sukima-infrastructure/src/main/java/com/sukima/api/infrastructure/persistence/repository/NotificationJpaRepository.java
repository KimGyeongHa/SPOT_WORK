package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.notification.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findAllByUserIdAndReadYnOrderByCreatedAtDesc(Long userId, String readYn);
}
