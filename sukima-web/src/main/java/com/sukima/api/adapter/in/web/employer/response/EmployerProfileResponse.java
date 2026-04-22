package com.sukima.api.adapter.in.web.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구인자 프로필 등록 응답")
public record EmployerProfileResponse(
        @Schema(description = "생성된 구인자 ID", example = "1") Long employerId
) {}
