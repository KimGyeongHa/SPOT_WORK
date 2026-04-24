package com.sukima.api.noshow;

import com.sukima.api.application.port.in.noshow.HandleNoShowUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis의 expired 이벤트를 수신한다.
 *
 * Redis에서 TTL이 만료되면 "__keyevent@0__:expired" 채널로
 * 만료된 key 이름이 message로 전달된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowExpirationListener implements MessageListener {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final HandleNoShowUseCase handleNoShowUseCase;

    @PostConstruct
    public void subscribe() {
        // __keyevent@0__:expired 채널 구독 (DB 0번의 expired 이벤트)
        redisMessageListenerContainer.addMessageListener(
                this,
                new PatternTopic("__keyevent@0__:expired")
        );
        log.info("NoShowExpirationListener 구독 시작");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        log.info("Redis 키 만료 이벤트 수신: {}", expiredKey);

        Long matchId = NoShowScheduler.extractMatchId(expiredKey);
        if (matchId == null) {
            return; // 노쇼 체크 키가 아니면 무시
        }

        try {
            handleNoShowUseCase.handle(matchId);
        } catch (Exception e) {
            log.error("노쇼 처리 실패: matchId={}", matchId, e);
        }
    }
}
