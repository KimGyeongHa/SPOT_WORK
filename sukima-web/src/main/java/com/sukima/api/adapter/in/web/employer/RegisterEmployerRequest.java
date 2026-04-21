package com.sukima.api.adapter.in.web.employer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고용주 프로필 등록 요청")
public record RegisterEmployerRequest(
        @Schema(description = "이름", example = "김사장")
        String name,

        @Schema(description = "전화번호", example = "010-9876-5432")
        String phone,

        @Schema(description = "회사명", example = "(주)스키마코리아")
        String companyName
) {}
