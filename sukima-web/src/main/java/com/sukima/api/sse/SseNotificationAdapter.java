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
        save(workerUserId, event, matchId);
        sseEmitterService.sendToUser(workerUserId, "MATCH_CONFIRMED", event);
    }

    @Override
    public void notifyNoShow(Long employerUserId, Long matchId, String workerName) {
        NotificationEvent event = NotificationEvent.noShowDetected(matchId, workerName);
        save(employerUserId, event, matchId);
        sseEmitterService.sendToUser(employerUserId, "NO_SHOW", event);
    }

    @Override
    public void notifyNewJobPosting(Long workerUserId, Long jobPostingId, String jobTitle) {
        NotificationEvent event = NotificationEvent.newJobPosting(jobPostingId, jobTitle);
        save(workerUserId, event, jobPostingId);
        sseEmitterService.sendToUser(workerUserId, "NEW_JOB_POSTING", event);
    }

    private void save(Long userId, NotificationEvent event, Long referenceId) {
        notificationJpaRepository.save(NotificationEntity.builder()
                .userId(userId)
                .type(event.type())
                .message(event.message())
                .referenceId(referenceId)
                .build());
    }
}
