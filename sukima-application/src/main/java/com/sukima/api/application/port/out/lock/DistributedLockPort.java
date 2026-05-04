package com.sukima.api.application.port.out.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 분산 락 포트.
 * 구현체는 Redisson 기반으로 동작한다.
 */
public interface DistributedLockPort {

    /**
     * 락을 획득하고 작업을 실행한다.
     *
     * @param lockKey   락 키
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 유지 시간 (작업 완료 전 자동 해제 방지)
     * @param timeUnit  시간 단위
     * @param supplier  락 획득 후 실행할 작업
     * @return 작업 결과
     */
    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier);
}
