package com.sukima.api.adapter.in.web.worker.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "체크인/체크아웃 요청")
public record CheckRequest(
        @Schema(description = "QR 코드에서 추출한 토큰")
        @NotBlank(message = "QR 토큰은 필수입니다.")
        String qrToken,

        @Schema(description = "현재 위치 위도", example = "37.4979")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        double latitude,

        @Schema(description = "현재 위치 경도", example = "127.0276")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        double longitude
) {}
