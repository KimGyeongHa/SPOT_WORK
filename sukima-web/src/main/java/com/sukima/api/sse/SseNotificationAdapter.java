package com.sukima.api.sse;

import com.sukima.api.application.port.out.notification.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseNotificationAdapter implements NotificationPort {

    private final SseEmitterService sseEmitterService;

    @Override
    public void notifyMatchConfirmed(Long workerUserId, Long matchId, String jobTitle) {
        sseEmitterService.sendToUser(
                workerUserId,
                "MATCH_CONFIRMED",
                NotificationEvent.matchConfirmed(matchId, jobTitle)
        );
    }

    @Override
    public void notifyNoShow(Long employerUserId, Long matchId, String workerName) {
        sseEmitterService.sendToUser(
                employerUserId,
                "NO_SHOW",
                NotificationEvent.noShowDetected(matchId, workerName)
        );
    }
}
