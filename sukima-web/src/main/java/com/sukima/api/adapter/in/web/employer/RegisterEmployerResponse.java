package com.sukima.api.adapter.in.web.employer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고용주 프로필 등록 응답")
public record RegisterEmployerResponse(
        @Schema(description = "생성된 고용주 프로필 ID", example = "1")
        Long employerId
) {}
