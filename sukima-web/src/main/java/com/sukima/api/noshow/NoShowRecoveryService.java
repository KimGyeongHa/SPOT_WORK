package com.sukima.api.noshow;

import com.sukima.api.application.port.in.noshow.HandleNoShowUseCase;
import com.sukima.api.infrastructure.persistence.entity.noshow.NoShowScheduleEntity;
import com.sukima.api.infrastructure.persistence.repository.NoShowScheduleJpaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowRecoveryService {

    private final NoShowScheduleJpaRepository noShowScheduleJpaRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final HandleNoShowUseCase handleNoShowUseCase;

    private static final String UNPROCESSED = "N";

    @PostConstruct
    public void recover() {
        LocalDateTime now = LocalDateTime.now();

        recoverPendingSchedules(now);
        recoverMissedSchedules(now);
    }

    /**
     * 미처리 + triggerAt 아직 안 지난 것 → Redis 재등록
     */
    private void recoverPendingSchedules(LocalDateTime now) {
        List<NoShowScheduleEntity> pending =
                noShowScheduleJpaRepository.findAllByProcessedYnAndTriggerAtAfter(UNPROCESSED, now);

        if (pending.isEmpty()) return;

        log.info("노쇼 스케줄 복구: {}건 Redis 재등록", pending.size());

        for (NoShowScheduleEntity schedule : pending) {
            long ttlSeconds = Math.max(1, Duration.between(now, schedule.getTriggerAt()).getSeconds());
            redisTemplate.opsForValue().set(
                    NoShowScheduler.KEY_PREFIX + schedule.getMatchId(),
                    String.valueOf(schedule.getMatchId()),
                    Duration.ofSeconds(ttlSeconds)
            );
            log.info("Redis 재등록: matchId={}, ttlSeconds={}", schedule.getMatchId(), ttlSeconds);
        }
    }

    /**
     * 미처리 + triggerAt 이미 지난 것 → 서버 다운 중 놓친 노쇼 → 즉시 처리
     */
    private void recoverMissedSchedules(LocalDateTime now) {
        List<NoShowScheduleEntity> missed =
                noShowScheduleJpaRepository.findAllByProcessedYnAndTriggerAtBefore(UNPROCESSED, now);

        if (missed.isEmpty()) return;

        log.warn("서버 다운 중 놓친 노쇼: {}건 즉시 처리", missed.size());

        for (NoShowScheduleEntity schedule : missed) {
            try {
                handleNoShowUseCase.handle(schedule.getMatchId());
                log.warn("누락 노쇼 처리 완료: matchId={}", schedule.getMatchId());
            } catch (Exception e) {
                log.error("누락 노쇼 처리 실패: matchId={}", schedule.getMatchId(), e);
            }
        }
    }
}
