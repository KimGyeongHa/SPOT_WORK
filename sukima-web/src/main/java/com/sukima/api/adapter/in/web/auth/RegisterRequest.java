package com.sukima.api.adapter.in.web.auth;

import com.sukima.api.domain.common.type.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public record RegisterRequest(
        @Schema(description = "이메일 주소", example = "user@example.com")
        String email,

        @Schema(description = "비밀번호", example = "password123!")
        String password,

        @Schema(description = "역할 (WORKER: 구직자, EMPLOYER: 고용주)", example = "WORKER")
        RoleType role
) {}
