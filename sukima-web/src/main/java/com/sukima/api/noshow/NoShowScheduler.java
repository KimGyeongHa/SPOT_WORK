package com.sukima.api.noshow;

import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 매칭 확정 시 노쇼 체크용 Redis 키를 TTL과 함께 저장한다.
 * 근무 시작 시간 + 15분 뒤에 TTL이 만료되면
 * Redis가 expired 이벤트를 발행하고 {@link NoShowExpirationListener}가 수신한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowScheduler implements NoShowSchedulerPort {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long GRACE_PERIOD_MINUTES = 15;
    private static final String KEY_PREFIX = "noshow:";

    @Override
    public void schedule(Long matchId, LocalDateTime workStartAt) {
        LocalDateTime triggerAt = workStartAt.plusMinutes(GRACE_PERIOD_MINUTES);
        LocalDateTime now = LocalDateTime.now();

        long ttlSeconds = Math.max(1, Duration.between(now, triggerAt).getSeconds());

        String key = KEY_PREFIX + matchId;
        redisTemplate.opsForValue().set(key, String.valueOf(matchId), Duration.ofSeconds(ttlSeconds));

        log.info("노쇼 체크 예약: matchId={}, triggerAt={}, ttlSeconds={}",
                matchId, triggerAt, ttlSeconds);
    }

    @Override
    public void cancel(Long matchId) {
        redisTemplate.delete(KEY_PREFIX + matchId);
        log.info("노쇼 체크 취소: matchId={}", matchId);
    }

    public static Long extractMatchId(String expiredKey) {
        if (expiredKey == null || !expiredKey.startsWith(KEY_PREFIX)) {
            return null;
        }
        return Long.parseLong(expiredKey.substring(KEY_PREFIX.length()));
    }
}
