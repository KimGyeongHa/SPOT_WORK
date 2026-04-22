package com.sukima.api.adapter.in.web.auth.request;

import com.sukima.api.domain.common.type.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public record RegisterRequest(
        @Schema(description = "이메일", example = "user@test.com") String email,
        @Schema(description = "비밀번호", example = "password1234") String password,
        @Schema(description = "역할", example = "WORKER") RoleType role
) {}
