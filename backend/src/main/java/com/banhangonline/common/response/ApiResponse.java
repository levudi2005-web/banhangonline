package com.banhangonline.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, String code, T data, Map<String, String> errors) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, null, data, null);
    }
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, message, code, null, null);
    }
    public static ApiResponse<Void> invalid(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, "VALIDATION_ERROR", null, errors);
    }
}
