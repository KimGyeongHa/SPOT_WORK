package com.sukima.api.domain.worklog;

import com.sukima.api.domain.worklog.type.WorkLogType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WorkLog {

    private final Long id;
    private final Long matchId;
    private final WorkLogType type;
    private final double latitude;
    private final double longitude;
    private final LocalDateTime scannedAt;

    @Builder
    public WorkLog(Long id, Long matchId, WorkLogType type,
                   double latitude, double longitude, LocalDateTime scannedAt) {
        this.id = id;
        this.matchId = matchId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.scannedAt = scannedAt;
    }
}
