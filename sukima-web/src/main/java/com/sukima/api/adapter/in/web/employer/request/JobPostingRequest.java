package com.sukima.api.adapter.in.web.employer.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "공고 등록 요청")
public record JobPostingRequest(
        @Schema(description = "공고 제목", example = "카페 알바 구합니다")
        @NotBlank(message = "공고 제목은 필수입니다.")
        @Size(max = 100, message = "공고 제목은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "공고 상세 설명", example = "주말 오전 카페 알바")
        String description,

        @Schema(description = "근무지 위도", example = "37.4979")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        double latitude,

        @Schema(description = "근무지 경도", example = "127.0276")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        double longitude,

        @Schema(description = "근무지 주소", example = "서울시 강남구 역삼동")
        @NotBlank(message = "근무지 주소는 필수입니다.")
        String address,

        @Schema(description = "시급 (원)", example = "10000")
        @Min(value = 0, message = "시급은 0원 이상이어야 합니다.")
        int hourlyWage,

        @Schema(description = "모집 정원", example = "3")
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
        int capacity,

        @Schema(description = "근무 시작 시간", example = "2026-05-01T09:00:00")
        @NotNull(message = "근무 시작 시간은 필수입니다.")
        @Future(message = "근무 시작 시간은 현재 시각 이후여야 합니다.")
        LocalDateTime startAt,

        @Schema(description = "근무 종료 시간", example = "2026-05-01T13:00:00")
        @NotNull(message = "근무 종료 시간은 필수입니다.")
        @Future(message = "근무 종료 시간은 현재 시각 이후여야 합니다.")
        LocalDateTime endAt
) {}
