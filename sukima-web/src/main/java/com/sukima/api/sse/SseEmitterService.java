package com.sukima.api.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결 관리 서비스.
 *
 * userId별로 SseEmitter를 관리하며, 단일 기기 로그인 정책에 따라
 * 1 유저 = 1 SSE 연결만 유지한다.
 * 새 연결 시 기존 연결은 자동 종료된다.
 */
@Slf4j
@Service
public class SseEmitterService {

    // userId → SseEmitter (1:1)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT = 60L * 60 * 1000; // 1시간

    /**
     * SSE 연결 생성
     */
    public SseEmitter connect(Long userId) {
        // 기존 연결 있으면 종료
        SseEmitter existing = emitters.get(userId);
        if (existing != null) {
            existing.complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("SSE 연결 종료: userId={}", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("SSE 연결 타임아웃: userId={}", userId);
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            log.warn("SSE 연결 에러: userId={}", userId, e);
        });

        emitters.put(userId, emitter);
        log.info("SSE 연결 생성: userId={}", userId);

        // 연결 직후 초기 이벤트 전송 (연결 확인용)
        sendToUser(userId, "connect", "SSE 연결 성공");

        return emitter;
    }

    /**
     * 특정 유저에게 이벤트 전송
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("SSE 미연결 상태: userId={}, event={}", userId, eventName);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.debug("SSE 이벤트 전송: userId={}, event={}", userId, eventName);
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("SSE 전송 실패, 연결 제거: userId={}", userId);
        }
    }

    /**
     * 연결 해제
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 연결 해제: userId={}", userId);
        }
    }

    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }
}
