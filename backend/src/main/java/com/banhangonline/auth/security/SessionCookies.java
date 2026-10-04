package com.banhangonline.auth.security;

import com.banhangonline.config.AppProperties;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.auth.dto.UserResponse;
import com.banhangonline.user.entity.User;
import com.banhangonline.auth.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class SessionCookies {
    private final SessionService sessions;
    private final AppProperties props;

    public SessionCookies(SessionService sessions, AppProperties props) {
        this.sessions = sessions;
        this.props = props;
    }

    /** Tạo phiên mới, huỷ phiên cũ (nếu có) và trả về response kèm cookie HttpOnly. */
    public ResponseEntity<ApiResponse<UserResponse>> start(User user, boolean remember, String message, HttpServletRequest req) {
        revoke(req);
        SessionService.Issued s = sessions.create(user, remember, req.getRemoteAddr(), req.getHeader("User-Agent"));
        ResponseCookie.ResponseCookieBuilder b = base(s.token());
        if (remember) b.maxAge(s.ttl());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, b.build().toString())
                .body(ApiResponse.ok(message, UserResponse.from(user)));
    }

    public void revoke(HttpServletRequest req) {
        Cookie c = WebUtils.getCookie(req, props.cookie().name());
        if (c != null) sessions.revoke(c.getValue());
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(props.cookie().name(), value)
                .httpOnly(true).secure(props.cookie().secure())
                .sameSite(props.cookie().sameSite()).path("/");
    }
}
