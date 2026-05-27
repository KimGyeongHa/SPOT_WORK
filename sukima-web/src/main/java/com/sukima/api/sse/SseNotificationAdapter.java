package com.sukima.api.sse;

import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.infrastructure.persistence.entity.notification.NotificationEntity;
import com.sukima.api.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseNotificationAdapter implements NotificationPort {

    private final SseEmitterService sseEmitterService;
    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public void notifyMatchConfirmed(Long workerUserId, Long matchId, String jobTitle) {
        NotificationEvent event = NotificationEvent.matchConfirmed(matchId, jobTitle);

        // DB 저장 (SSE 미연결 시 유실 방지)
        notificationJpaRepository.save(NotificationEntity.builder()
                .userId(workerUserId)
                .type(event.type())
                .message(event.message())
                .referenceId(matchId)
                .build());

        sseEmitterService.sendToUser(workerUserId, "MATCH_CONFIRMED", event);
    }

    @Override
    public void notifyNoShow(Long employerUserId, Long matchId, String workerName) {
        NotificationEvent event = NotificationEvent.noShowDetected(matchId, workerName);

        notificationJpaRepository.save(NotificationEntity.builder()
                .userId(employerUserId)
                .type(event.type())
                .message(event.message())
                .referenceId(matchId)
                .build());

        sseEmitterService.sendToUser(employerUserId, "NO_SHOW", event);
    }
}
