package com.sukima.api.application.service;

import com.sukima.api.application.port.in.job.CreateJobPostingUseCase;
import com.sukima.api.application.port.in.job.GetNearbyJobPostingsUseCase;
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

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public Long create(CreateJobPostingUseCase.Command command) {
        EmployerEntity employer = employerJpaRepository.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("구인자 프로필이 등록되지 않았습니다."));

        Point location = geometryFactory.createPoint(
                new Coordinate(command.longitude(), command.latitude())
        );

        JobPostingEntity entity = JobPostingEntity.builder()
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
                .build();

        return jobPostingJpaRepository.save(entity).getId();
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
