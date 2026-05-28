package com.sukima.api.application.service;

import com.sukima.api.application.port.in.job.CreateJobPostingUseCase;
import com.sukima.api.application.port.in.job.GetNearbyJobPostingsUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.job.JobPosting;
import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.repository.EmployerJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.JobPostingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobPostingService implements CreateJobPostingUseCase, GetNearbyJobPostingsUseCase {

    private final JobPostingJpaRepository jobPostingJpaRepository;
    private final EmployerJpaRepository employerJpaRepository;
    private final JobPostingNotificationBatch jobPostingNotificationBatch;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public Long create(CreateJobPostingUseCase.Command command) {
        EmployerEntity employer = employerJpaRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYER_NOT_FOUND));

        if (employer.isPenalized()) {
            throw new BusinessException(ErrorCode.EMPLOYER_PENALIZED);
        }

        Point location = geometryFactory.createPoint(
                new Coordinate(command.longitude(), command.latitude())
        );

        JobPostingEntity saved = jobPostingJpaRepository.save(JobPostingEntity.builder()
                .employer(employer)
                .title(command.title())
                .description(command.description())
                .location(location)
                .address(command.address())
                .hourlyWage(command.hourlyWage())
                .capacity(command.capacity())
                .startAt(command.startAt())
                .endAt(command.endAt())
                .status(JobStatus.OPEN)
                .build());

        // 공고 위치 기준 반경 내 Worker에게 즉시 알림
        jobPostingNotificationBatch.notifyNearbyWorkers(saved.getId(), saved.getTitle(), location);

        return saved.getId();
    }

    @Override
    public List<JobPosting> getNearby(GetNearbyJobPostingsUseCase.Query query) {
        return jobPostingJpaRepository
                .findNearby(query.latitude(), query.longitude(), query.radiusMeters())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private JobPosting toDomain(JobPostingEntity entity) {
        return JobPosting.builder()
                .id(entity.getId())
                .employerId(entity.getEmployer().getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .latitude(entity.getLocation().getY())
                .longitude(entity.getLocation().getX())
                .address(entity.getAddress())
                .hourlyWage(entity.getHourlyWage())
                .capacity(entity.getCapacity())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .status(entity.getStatus())
                .build();
    }
}
