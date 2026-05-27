package com.sukima.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sukima.api.domain.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공통 API 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "성공 여부") boolean success,
        @Schema(description = "응답 데이터") T data,
        @Schema(description = "에러 정보") ErrorDetail error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), null, LocalDateTime.now()));
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(errorCode.getCode(), message, null, LocalDateTime.now()));
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), fieldErrors, LocalDateTime.now()));
    }

    @Schema(description = "에러 상세")
    public record ErrorDetail(
            String code,
            String message,
            List<FieldError> fieldErrors,
            LocalDateTime timestamp
    ) {}

    @Schema(description = "필드 에러")
    public record FieldError(
            String field,
            String value,
            String reason
    ) {}
}
