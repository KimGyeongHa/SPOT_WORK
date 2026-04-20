package com.sukima.api.domain.job;

import com.sukima.api.domain.job.type.JobStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class JobPosting {

    private final Long id;
    private final Long employerId;
    private final String title;
    private final String description;
    private final double latitude;
    private final double longitude;
    private final String address;
    private final int hourlyWage;
    private final int capacity;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final JobStatus status;

    @Builder
    public JobPosting(Long id, Long employerId, String title, String description,
                      double latitude, double longitude, String address,
                      int hourlyWage, int capacity,
                      LocalDateTime startAt, LocalDateTime endAt, JobStatus status) {
        this.id = id;
        this.employerId = employerId;
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.hourlyWage = hourlyWage;
        this.capacity = capacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }
}
