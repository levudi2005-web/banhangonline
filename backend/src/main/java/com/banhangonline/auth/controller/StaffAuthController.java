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
@RequestMapping("/api/auth")
public class StaffAuthController {
    private final AuthService auth;
    private final SessionCookies cookies;

    public StaffAuthController(AuthService auth, SessionCookies cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse<UserResponse>> staffLogin(
            @Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        User u = auth.authenticateStaff(r.account(), r.password());
        return cookies.start(u, r.remember(), "Đăng nhập nhân viên thành công", req);
    }

    @PostMapping("/owner/login")
    public ResponseEntity<ApiResponse<UserResponse>> ownerLogin(
            @Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        User u = auth.authenticateOwner(r.account(), r.password());
        return cookies.start(u, r.remember(), "Đăng nhập chủ cửa hàng thành công", req);
    }
}
