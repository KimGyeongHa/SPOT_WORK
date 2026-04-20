package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.application.JobApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationJpaRepository extends JpaRepository<JobApplicationEntity, Long> {

    List<JobApplicationEntity> findByJobPostingId(Long jobPostingId);
    boolean existsByJobPostingIdAndWorkerId(Long jobPostingId, Long workerId);
    int countByJobPostingIdAndStatus(Long jobPostingId, String status);
}
