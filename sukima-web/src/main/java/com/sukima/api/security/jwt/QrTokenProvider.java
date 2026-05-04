package com.sukima.api.security.jwt;

import com.sukima.api.application.port.out.qr.QrTokenPort;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * QR 체크인용 JWT 발급/검증.
 *
 * 인증용 JWT(JwtTokenProvider)와 별개의 시크릿 키를 사용해 분리 관리한다.
 * 토큰 만료 시간은 근무 종료 + 30분으로 설정한다.
 */
@Component
public class QrTokenProvider implements QrTokenPort {

    private final SecretKey secretKey;
    private static final long EXPIRATION_BUFFER_MINUTES = 30;

    public QrTokenProvider(@Value("${qr.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public String generate(Long matchId, Long workerId, Long version, LocalDateTime workEndAt) {
        Date expiration = Date.from(
                workEndAt.plusMinutes(EXPIRATION_BUFFER_MINUTES)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        return Jwts.builder()
                .subject(String.valueOf(matchId))
                .claim("workerId", workerId)
                .claim("version", version)
                .claim("type", "qr")
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public QrTokenInfo parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!"qr".equals(claims.get("type", String.class))) {
                throw new BusinessException(ErrorCode.INVALID_QR_TOKEN);
            }

            return new QrTokenInfo(
                    Long.parseLong(claims.getSubject()),
                    claims.get("workerId", Long.class),
                    claims.get("version", Long.class)
            );
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_QR_TOKEN);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_QR_TOKEN);
        }
    }
}
