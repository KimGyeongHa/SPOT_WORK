package com.sukima.api.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * AccessToken Redis 관리 서비스.
 *
 * AccessToken을 Redis에 저장하여 단일 기기 로그인을 보장한다.
 * 새 기기에서 로그인 시 기존 AccessToken을 덮어씌워 이전 기기의 접근을 차단한다.
 * 매 요청마다 Redis에 저장된 AccessToken과 일치 여부를 확인한다.
 */
@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String PREFIX = "access:";

    public void save(Long userId, String accessToken) {
        redisTemplate.opsForValue().set(
                PREFIX + userId,
                accessToken,
                jwtTokenProvider.getAccessExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    public boolean validate(Long userId, String accessToken) {
        String saved = redisTemplate.opsForValue().get(PREFIX + userId);
        return saved != null && saved.equals(accessToken);
    }

    public void delete(Long userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
