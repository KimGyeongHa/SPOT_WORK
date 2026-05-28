package com.sukima.api.infrastructure.persistence.repository;

import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkerJpaRepository extends JpaRepository<WorkerEntity, Long> {

    Optional<WorkerEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);

    // 공고 위치 기준 반경 내 알림 활성화된 Worker 조회
    // Worker의 notification 위치가 공고 위치의 반경 내에 있는지 체크
    @Query(value = """
            SELECT * FROM workers
            WHERE notification_enabled = true
            AND notification_lat IS NOT NULL
            AND ST_DWithin(
                ST_SetSRID(ST_MakePoint(notification_lng, notification_lat), 4326)::geography,
                ST_SetSRID(ST_MakePoint(:jobLng, :jobLat), 4326)::geography,
                notification_radius_meters
            )
            """, nativeQuery = true)
    List<WorkerEntity> findWorkersInRangeOf(@Param("jobLat") double jobLat,
                                             @Param("jobLng") double jobLng);
}
