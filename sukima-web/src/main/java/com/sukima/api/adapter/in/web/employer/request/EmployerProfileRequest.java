package com.sukima.api.adapter.in.web.employer.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "구인자 프로필 등록 요청")
public record EmployerProfileRequest(
        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "연락처", example = "010-1234-5678")
        @NotBlank(message = "연락처는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")
        String phone,

        @Schema(description = "회사명", example = "스키마카페")
        @NotBlank(message = "회사명은 필수입니다.")
        @Size(max = 100, message = "회사명은 100자 이하여야 합니다.")
        String companyName
) {}
