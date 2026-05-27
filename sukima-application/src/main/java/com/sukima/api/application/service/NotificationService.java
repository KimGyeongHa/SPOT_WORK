package com.sukima.api.application.service;

import com.sukima.api.application.port.in.notification.GetNotificationsUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.infrastructure.persistence.entity.notification.NotificationEntity;
import com.sukima.api.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService implements GetNotificationsUseCase {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public List<NotificationInfo> getUnread(Long userId) {
        return notificationJpaRepository
                .findAllByUserIdAndReadYnOrderByCreatedAtDesc(userId, "N")
                .stream()
                .map(entity -> new NotificationInfo(
                        entity.getId(),
                        entity.getType(),
                        entity.getMessage(),
                        entity.getReferenceId(),
                        entity.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationEntity notification = notificationJpaRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        notification.markRead();
    }
}
