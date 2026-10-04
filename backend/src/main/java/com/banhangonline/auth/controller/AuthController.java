package com.banhangonline.auth.controller;

import com.banhangonline.auth.dto.ForgotPasswordRequest;
import com.banhangonline.auth.dto.LoginRequest;
import com.banhangonline.auth.dto.RegisterRequest;
import com.banhangonline.auth.dto.ResetPasswordRequest;
import com.banhangonline.auth.dto.UserResponse;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.user.entity.User;
import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.auth.security.SessionCookies;
import com.banhangonline.auth.service.AuthService;
import com.banhangonline.auth.service.PasswordResetService;
import com.banhangonline.auth.service.PasswordRecoveryService;
import com.banhangonline.auth.dto.PasswordResetTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final PasswordResetService resets;
    private final PasswordRecoveryService recovery;
    private final SessionCookies cookies;

    public AuthController(
            AuthService auth, PasswordResetService resets, PasswordRecoveryService recovery,
            SessionCookies cookies) {
        this.auth = auth;
        this.resets = resets;
        this.recovery = recovery;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        User u = auth.authenticate(r.account(), r.password());
        return cookies.start(u, r.remember(), "Đăng nhập thành công", req);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest r) {
        User u = auth.registerCustomer(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Đăng ký thành công", UserResponse.from(u)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest req) {
        cookies.revoke(req);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .body(ApiResponse.<Void>ok("Đã đăng xuất", null));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok("OK", UserResponse.from(auth.findUser(principal.userId())));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<PasswordResetTokenResponse> forgot(@Valid @RequestBody ForgotPasswordRequest r) {
        String token = recovery.verifyPhoneAndIssueToken(r.account(), r.phoneLastFour());
        return ApiResponse.ok("Thông tin xác minh hợp lệ. Bạn có thể đặt mật khẩu mới.",
                new PasswordResetTokenResponse(token));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest r) {
        resets.reset(r.token(), r.newPassword(), r.confirmPassword());
        return ApiResponse.<Void>ok("Đã đặt lại mật khẩu. Vui lòng đăng nhập lại.", null);
    }

}
