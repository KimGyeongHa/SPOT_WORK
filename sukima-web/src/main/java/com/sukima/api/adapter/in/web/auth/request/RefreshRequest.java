package com.sukima.api.adapter.in.web.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급/로그아웃 요청")
public record RefreshRequest(
        @Schema(description = "리프레시 토큰")
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {}
