package com.banhangonline.common.security;

import com.banhangonline.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Ghi lỗi JSON từ filter (nơi @RestControllerAdvice không áp dụng). */
public final class ErrorWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private ErrorWriter() {}

    public static void write(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(res.getWriter(), ApiResponse.error(code, message));
    }
}
