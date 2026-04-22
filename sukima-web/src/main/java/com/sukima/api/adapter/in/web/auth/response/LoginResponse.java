package com.sukima.api.adapter.in.web.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "액세스 토큰 (15분)") String accessToken,
        @Schema(description = "리프레시 토큰 (7일)") String refreshToken
) {}
