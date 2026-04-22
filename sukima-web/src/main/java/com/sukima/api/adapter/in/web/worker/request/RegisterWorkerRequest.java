package com.sukima.api.adapter.in.web.worker.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구직자 프로필 등록 요청")
public record RegisterWorkerRequest(
        @Schema(description = "이름", example = "홍길동") String name,
        @Schema(description = "전화번호", example = "010-1234-5678") String phone
) {}
