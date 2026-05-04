package com.sukima.api.adapter.in.web.worker.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "QR 토큰 발급 응답")
public record QrTokenResponse(
        @Schema(description = "QR 코드에 담을 토큰") String qrToken,
        @Schema(description = "토큰 만료 시각") LocalDateTime expiresAt
) {}
