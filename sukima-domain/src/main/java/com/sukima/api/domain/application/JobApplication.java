package com.sukima.api.domain.application;

import com.sukima.api.domain.application.type.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class JobApplication {

    private final Long id;
    private final Long jobPostingId;
    private final Long workerId;
    private final ApplicationStatus status;
    private final LocalDateTime appliedAt;

    @Builder
    public JobApplication(Long id, Long jobPostingId, Long workerId,
                          ApplicationStatus status, LocalDateTime appliedAt) {
        this.id = id;
        this.jobPostingId = jobPostingId;
        this.workerId = workerId;
        this.status = status;
        this.appliedAt = appliedAt;
    }
}
