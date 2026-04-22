package com.sukima.api.adapter.in.web.employer.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구인자 프로필 등록 요청")
public record EmployerProfileRequest(
        @Schema(description = "이름", example = "홍길동") String name,
        @Schema(description = "연락처", example = "010-1234-5678") String phone,
        @Schema(description = "회사명", example = "스키마카페") String companyName
) {}
