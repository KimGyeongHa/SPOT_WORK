package com.sukima.api.adapter.in.web.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급/로그아웃 요청")
public record RefreshRequest(
        @Schema(description = "리프레시 토큰") String refreshToken
) {}
