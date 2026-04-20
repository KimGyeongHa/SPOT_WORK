package com.sukima.api.domain.match;

import com.sukima.api.domain.match.type.MatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Match {

    private final Long id;
    private final Long applicationId;
    private final Long jobPostingId;
    private final Long workerId;
    private final MatchStatus status;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime completedAt;

    @Builder
    public Match(Long id, Long applicationId, Long jobPostingId, Long workerId,
                 MatchStatus status, LocalDateTime confirmedAt, LocalDateTime completedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.jobPostingId = jobPostingId;
        this.workerId = workerId;
        this.status = status;
        this.confirmedAt = confirmedAt;
        this.completedAt = completedAt;
    }
}
