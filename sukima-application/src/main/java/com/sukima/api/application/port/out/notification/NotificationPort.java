package com.sukima.api.application.port.out.notification;

public interface NotificationPort {

    void notifyMatchConfirmed(Long workerUserId, Long matchId, String jobTitle);

    void notifyNoShow(Long employerUserId, Long matchId, String workerName);

    void notifyNewJobPosting(Long workerUserId, Long jobPostingId, String jobTitle);

    void notifyPenaltyExpired(Long userId, String message);
}
