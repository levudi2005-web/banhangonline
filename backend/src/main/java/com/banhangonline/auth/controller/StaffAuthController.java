package com.banhangonline.auth.controller;

import com.banhangonline.auth.dto.LoginRequest;
import com.banhangonline.auth.dto.UserResponse;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.user.entity.User;
import com.banhangonline.auth.security.SessionCookies;
import com.banhangonline.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/staff")
public class StaffAuthController {
    private final AuthService auth;
    private final SessionCookies cookies;

    public StaffAuthController(AuthService auth, SessionCookies cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        User u = auth.authenticateStaff(r.account(), r.password());
        return cookies.start(u, r.remember(), "Đăng nhập quản trị thành công", req);
    }

}
