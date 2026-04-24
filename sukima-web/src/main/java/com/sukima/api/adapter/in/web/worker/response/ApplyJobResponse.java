package com.sukima.api.adapter.in.web.worker.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공고 지원 응답")
public record ApplyJobResponse(
        @Schema(description = "생성된 지원 ID", example = "1") Long applicationId
) {}
