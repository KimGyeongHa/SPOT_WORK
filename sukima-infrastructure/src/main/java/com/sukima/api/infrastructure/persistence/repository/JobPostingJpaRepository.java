package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobPostingJpaRepository extends JpaRepository<JobPostingEntity, Long> {

    @Query(value = """
            SELECT * FROM job_postings
            WHERE status = 'OPEN'
            AND ST_DWithin(
                location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radius
            )
            """, nativeQuery = true)
    List<JobPostingEntity> findNearby(@Param("lat") double lat,
                                      @Param("lng") double lng,
                                      @Param("radius") double radius);

    // 배치 알림용: 특정 시간 이후 등록된 근처 OPEN 공고 조회
    @Query(value = """
            SELECT * FROM job_postings
            WHERE status = 'OPEN'
            AND created_at >= :since
            AND ST_DWithin(
                location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radius
            )
            """, nativeQuery = true)
    List<JobPostingEntity> findNearbyCreatedAfter(@Param("lat") double lat,
                                                   @Param("lng") double lng,
                                                   @Param("radius") double radius,
                                                   @Param("since") LocalDateTime since);
}
