package com.banhangonline.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

/**
 * Chiến lược CSRF: mọi request thay đổi dữ liệu dưới /api phải có header X-Requested-With.
 * Form HTML của trang khác không thể gắn header tuỳ biến; fetch từ origin khác phải qua CORS preflight
 * và chỉ origin được cấu hình mới qua được. Kết hợp cookie SameSite.
 */
public class RequestedWithFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("X-Requested-With");
        if (MUTATING.contains(req.getMethod()) && req.getRequestURI().startsWith("/api/") && (h == null || h.isBlank())) {
            ErrorWriter.write(res, 403, "CSRF_BLOCKED", "Yêu cầu không hợp lệ");
            return;
        }
        chain.doFilter(req, res);
    }
}
