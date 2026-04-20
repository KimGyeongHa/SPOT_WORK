package com.sukima.api.application.port.in.job;

import com.sukima.api.domain.job.JobPosting;

import java.util.List;

public interface GetNearbyJobPostingsUseCase {

    List<JobPosting> getNearby(Query query);

    record Query(double latitude, double longitude, double radiusMeters) {}
}
