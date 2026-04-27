package com.sukima.api.noshow;

import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
import com.sukima.api.infrastructure.persistence.entity.noshow.NoShowScheduleEntity;
import com.sukima.api.infrastructure.persistence.repository.NoShowScheduleJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowScheduler implements NoShowSchedulerPort {

    private final RedisTemplate<String, String> redisTemplate;
    private final NoShowScheduleJpaRepository noShowScheduleJpaRepository;

    private static final long GRACE_PERIOD_MINUTES = 15;
    static final String KEY_PREFIX = "noshow:";

    @Override
    @Transactional
    public void schedule(Long matchId, LocalDateTime workStartAt) {
        LocalDateTime triggerAt = workStartAt.plusMinutes(GRACE_PERIOD_MINUTES);
        LocalDateTime now = LocalDateTime.now();
        long ttlSeconds = Math.max(1, Duration.between(now, triggerAt).getSeconds());

        redisTemplate.opsForValue().set(KEY_PREFIX + matchId, String.valueOf(matchId), Duration.ofSeconds(ttlSeconds));

        noShowScheduleJpaRepository.save(
                NoShowScheduleEntity.builder()
                        .matchId(matchId)
                        .triggerAt(triggerAt)
                        .createdAt(now)
                        .build()
        );

        log.info("노쇼 체크 예약: matchId={}, triggerAt={}, ttlSeconds={}", matchId, triggerAt, ttlSeconds);
    }

    @Override
    @Transactional
    public void cancel(Long matchId) {
        redisTemplate.delete(KEY_PREFIX + matchId);

        noShowScheduleJpaRepository.findByMatchId(matchId)
                .ifPresent(schedule -> {
                    schedule.markProcessed();
                    log.info("노쇼 체크 취소 (정상 체크인): matchId={}", matchId);
                });
    }

    public static Long extractMatchId(String expiredKey) {
        if (expiredKey == null || !expiredKey.startsWith(KEY_PREFIX)) {
            return null;
        }
        return Long.parseLong(expiredKey.substring(KEY_PREFIX.length()));
    }
}
