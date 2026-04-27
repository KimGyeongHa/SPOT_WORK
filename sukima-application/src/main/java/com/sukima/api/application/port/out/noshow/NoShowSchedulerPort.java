package com.sukima.api.application.port.out.noshow;

import java.time.LocalDateTime;

/**
 * 노쇼 체크를 외부 스케줄링 시스템에 예약/취소하기 위한 포트.
 * 구현체는 Redis TTL + DB 백업 방식으로 동작한다.
 */
public interface NoShowSchedulerPort {

    // Redis TTL 등록 + DB 백업 저장
    void schedule(Long matchId, LocalDateTime workStartAt);

    // Redis 키 삭제 + DB 백업 삭제
    void cancel(Long matchId);
}
