package com.sukima.api.application.port.out.notification;

public interface NotificationPort {

    // 매칭 확정 시 Worker에게 알림
    void notifyMatchConfirmed(Long workerUserId, Long matchId, String jobTitle);

    // 노쇼 감지 시 Employer에게 알림
    void notifyNoShow(Long employerUserId, Long matchId, String workerName);

    // 배치: 반경 내 신규 공고 알림
    void notifyNewJobPosting(Long workerUserId, Long jobPostingId, String jobTitle);
}
