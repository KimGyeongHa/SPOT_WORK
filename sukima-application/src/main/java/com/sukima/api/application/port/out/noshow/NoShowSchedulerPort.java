package com.sukima.api.application.port.out.noshow;

import java.time.LocalDateTime;

/**
 * 노쇼 체크를 외부 스케줄링 시스템에 예약/취소하기 위한 포트.
 * 구현체는 Redis TTL 기반으로 동작한다. (NoShowSchedulerAdapter)
 */
public interface NoShowSchedulerPort {

    void schedule(Long matchId, LocalDateTime workStartAt);

    void cancel(Long matchId);
}
