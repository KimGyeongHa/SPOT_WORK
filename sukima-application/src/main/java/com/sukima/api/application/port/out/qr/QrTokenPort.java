package com.sukima.api.application.port.out.qr;

import java.time.LocalDateTime;

/**
 * QR 토큰 발급/검증을 위한 아웃바운드 포트.
 * 구현체는 web 모듈의 QrTokenProvider (JWT 기반).
 */
public interface QrTokenPort {

    /**
     * QR 토큰 발급
     */
    String generate(Long matchId, Long workerId, Long version, LocalDateTime workEndAt);

    /**
     * 토큰에서 추출한 정보
     */
    QrTokenInfo parse(String token);

    record QrTokenInfo(Long matchId, Long workerId, Long version) {}
}
