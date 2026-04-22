package com.sukima.api.adapter.in.web.employer.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공고 등록 요청")
public record JobPostingRequest(
        @Schema(description = "공고 제목", example = "카페 알바 구합니다") String title,
        @Schema(description = "공고 상세 설명", example = "주말 오전 카페 알바") String description,
        @Schema(description = "근무지 위도", example = "37.4979") double latitude,
        @Schema(description = "근무지 경도", example = "127.0276") double longitude,
        @Schema(description = "근무지 주소", example = "서울시 강남구 역삼동") String address,
        @Schema(description = "시급 (원)", example = "10000") int hourlyWage,
        @Schema(description = "모집 정원", example = "3") int capacity,
        @Schema(description = "근무 시작 시간", example = "2026-05-01T09:00:00") LocalDateTime startAt,
        @Schema(description = "근무 종료 시간", example = "2026-05-01T13:00:00") LocalDateTime endAt
) {}
