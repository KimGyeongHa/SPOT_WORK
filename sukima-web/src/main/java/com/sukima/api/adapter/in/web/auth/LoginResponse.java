package com.sukima.api.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인/토큰 갱신 응답")
public record LoginResponse(
        @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {}
