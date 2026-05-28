package com.sukima.api.adapter.in.web.worker.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "공고 알림 설정 요청")
public record NotificationSettingRequest(
        @Schema(description = "알림 활성화 여부") boolean enabled,
        @Schema(description = "기준 위도", example = "37.4979")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double lat,
        @Schema(description = "기준 경도", example = "127.0276")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double lng,
        @Schema(description = "알림 반경 (미터)", example = "3000")
        @Min(100) @Max(10000) Integer radiusMeters
) {}
