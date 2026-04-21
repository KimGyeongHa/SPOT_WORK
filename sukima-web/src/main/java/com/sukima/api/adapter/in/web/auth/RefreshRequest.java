package com.sukima.api.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신/로그아웃 요청")
public record RefreshRequest(
        @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {}
