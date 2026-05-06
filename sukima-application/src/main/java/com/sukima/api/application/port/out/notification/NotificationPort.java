package com.sukima.api.application.port.out.notification;

/**
 * 실시간 알림 아웃바운드 포트.
 * 구현체는 SSE 기반으로 동작한다.
 */
public interface NotificationPort {

    /**
     * 매칭 확정 시 Worker에게 알림
     */
    void notifyMatchConfirmed(Long workerUserId, Long matchId, String jobTitle);

    /**
     * 노쇼 감지 시 Employer에게 알림
     */
    void notifyNoShow(Long employerUserId, Long matchId, String workerName);
}
