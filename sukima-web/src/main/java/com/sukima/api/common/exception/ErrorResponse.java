package com.sukima.api.common.exception;

import com.sukima.api.domain.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "U001") String code,
        @Schema(description = "에러 메시지", example = "이미 사용 중인 이메일입니다.") String message,
        @Schema(description = "필드 에러 상세") List<FieldError> errors,
        @Schema(description = "발생 시각") LocalDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors, LocalDateTime.now());
    }

    public record FieldError(
            @Schema(description = "필드명") String field,
            @Schema(description = "입력값") String value,
            @Schema(description = "검증 실패 사유") String reason
    ) {}
}
