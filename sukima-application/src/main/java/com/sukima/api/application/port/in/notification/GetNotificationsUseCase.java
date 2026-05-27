package com.sukima.api.application.port.in.notification;

import java.time.LocalDateTime;
import java.util.List;

public interface GetNotificationsUseCase {

    List<NotificationInfo> getUnread(Long userId);

    void markAsRead(Long notificationId, Long userId);

    record NotificationInfo(
            Long id,
            String type,
            String message,
            Long referenceId,
            LocalDateTime createdAt
    ) {}
}
