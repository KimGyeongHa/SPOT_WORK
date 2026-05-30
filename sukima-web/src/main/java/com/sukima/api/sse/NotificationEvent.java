package com.sukima.api.sse;

import java.time.LocalDateTime;

public record NotificationEvent(
        String type,
        String message,
        Long referenceId,
        LocalDateTime timestamp
) {
    public static NotificationEvent matchConfirmed(Long matchId, String jobTitle) {
        return new NotificationEvent("MATCH_CONFIRMED",
                "'" + jobTitle + "' 공고에 매칭되었습니다!", matchId, LocalDateTime.now());
    }

    public static NotificationEvent noShowDetected(Long matchId, String workerName) {
        return new NotificationEvent("NO_SHOW",
                "'" + workerName + "' 님이 노쇼 처리되었습니다. 공고가 다시 오픈됩니다.",
                matchId, LocalDateTime.now());
    }

    public static NotificationEvent newJobPosting(Long jobPostingId, String jobTitle) {
        return new NotificationEvent("NEW_JOB_POSTING",
                "'" + jobTitle + "' 공고가 근처에 등록되었습니다!", jobPostingId, LocalDateTime.now());
    }

    public static NotificationEvent penaltyExpired(String message) {
        return new NotificationEvent("PENALTY_EXPIRED", message, null, LocalDateTime.now());
    }
}
