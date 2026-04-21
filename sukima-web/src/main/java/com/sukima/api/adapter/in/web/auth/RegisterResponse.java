package com.sukima.api.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record RegisterResponse(
        @Schema(description = "생성된 사용자 ID", example = "1")
        Long userId
) {}
