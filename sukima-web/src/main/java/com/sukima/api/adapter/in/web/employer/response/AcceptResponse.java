package com.sukima.api.adapter.in.web.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 수락 응답")
public record AcceptResponse(
        @Schema(description = "생성된 매칭 ID") Long matchId
) {}
