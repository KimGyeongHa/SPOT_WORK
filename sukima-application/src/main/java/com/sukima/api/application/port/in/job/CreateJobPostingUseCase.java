package com.sukima.api.application.port.in.job;

import java.time.LocalDateTime;

public interface CreateJobPostingUseCase {

    Long create(Command command);

    record Command(
            Long employerId,
            String title,
            String description,
            double latitude,
            double longitude,
            String address,
            int hourlyWage,
            int capacity,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {}
}
