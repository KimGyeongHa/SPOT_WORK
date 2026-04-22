package com.sukima.api.adapter.in.web.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record RegisterResponse(
        @Schema(description = "생성된 유저 ID", example = "1") Long userId
) {}
