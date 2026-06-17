package com.hotelchain.pro.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Chuẩn API Response wrapper cho toàn hệ thống.
 * Success: success=true, data=payload
 * Error: success=false, errors=list of field errors
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private List<FieldError> errors;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    @Builder.Default
    private String requestId = UUID.randomUUID().toString();

    // ===== Factory Methods =====

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("Thao tác thành công")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("CREATED")
                .message("Tạo mới thành công")
                .data(data)
                .build();
    }

    public static ApiResponse<Void> noContent(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .build();
    }

    public static ApiResponse<Void> error(String code, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }

    public static ApiResponse<Void> validationError(List<FieldError> errors) {
        return ApiResponse.<Void>builder()
                .success(false)
                .code("VALIDATION_ERROR")
                .message("Dữ liệu không hợp lệ")
                .errors(errors)
                .build();
    }

    // ===== Inner classes =====

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
