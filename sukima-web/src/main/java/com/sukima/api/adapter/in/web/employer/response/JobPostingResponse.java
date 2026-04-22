package com.sukima.api.adapter.in.web.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공고 등록 응답")
public record JobPostingResponse(
        @Schema(description = "생성된 공고 ID", example = "1") Long jobPostingId
) {}
