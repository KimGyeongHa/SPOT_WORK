package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobPostingJpaRepository extends JpaRepository<JobPostingEntity, Long> {

    @Query(value = """
            SELECT * FROM job_postings
            WHERE status = 'OPEN'
            AND ST_DWithin(
                location::geography,
                ST_MakePoint(:lng, :lat)::geography,
                :radius
            )
            """, nativeQuery = true)
    List<JobPostingEntity> findNearby(@Param("lat") double lat,
                                      @Param("lng") double lng,
                                      @Param("radius") double radius);
}
