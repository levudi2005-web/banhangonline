package com.banhangonline.common.security;

import com.banhangonline.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Giới hạn số lần gọi các endpoint nhạy cảm theo IP (bộ nhớ của một instance). */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED = Set.of(
            "/api/auth/login", "/api/auth/staff/login", "/api/auth/register",
            "/api/auth/staff/register", "/api/auth/forgot-password", "/api/auth/reset-password",
            "/api/auth/otp/send", "/api/auth/otp/resend", "/api/auth/otp/verify");

    private final Map<String, long[]> hits = new ConcurrentHashMap<>();
    private final int max;
    private final long windowMs;

    public RateLimitFilter(AppProperties props) {
        this.max = props.rateLimit().maxAttempts();
        this.windowMs = props.rateLimit().windowSeconds() * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(req.getMethod()) && LIMITED.contains(req.getRequestURI())) {
            if (hits.size() > 50_000) hits.clear();
            long now = System.currentTimeMillis();
            String path = req.getRequestURI().startsWith("/api/auth/otp/")
                    ? "/api/auth/otp"
                    : req.getRequestURI();
            long[] w = hits.compute(req.getRemoteAddr() + "|" + path, (k, v) -> {
                if (v == null || now - v[0] >= windowMs) return new long[]{now, 1};
                v[1]++;
                return v;
            });
            if (w[1] > max) {
                ErrorWriter.write(res, 429, "RATE_LIMITED", "Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút.");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
