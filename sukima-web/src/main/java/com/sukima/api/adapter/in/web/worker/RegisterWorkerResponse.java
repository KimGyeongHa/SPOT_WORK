package com.sukima.api.adapter.in.web.worker;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구직자 프로필 등록 응답")
public record RegisterWorkerResponse(
        @Schema(description = "생성된 구직자 프로필 ID", example = "1")
        Long workerId
) {}
