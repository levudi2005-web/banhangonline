package com.banhangonline.auth.controller;

import com.banhangonline.auth.dto.ForgotPasswordRequest;
import com.banhangonline.auth.dto.LoginRequest;
import com.banhangonline.auth.dto.OtpSendRequest;
import com.banhangonline.auth.dto.OtpVerificationResponse;
import com.banhangonline.auth.dto.OtpVerifyRequest;
import com.banhangonline.auth.dto.RegisterRequest;
import com.banhangonline.auth.dto.ResetPasswordRequest;
import com.banhangonline.auth.dto.UserResponse;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.user.entity.User;
import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.auth.security.SessionCookies;
import com.banhangonline.auth.service.AuthService;
import com.banhangonline.auth.service.PasswordResetService;
import com.banhangonline.auth.service.OtpService;
import com.banhangonline.auth.entity.OtpPurpose;
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
    private final SessionCookies cookies;
    private final OtpService otps;

    public AuthController(
            AuthService auth, PasswordResetService resets, SessionCookies cookies, OtpService otps) {
        this.auth = auth;
        this.resets = resets;
        this.cookies = cookies;
        this.otps = otps;
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
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest r) {
        resets.request(r.account());
        return ApiResponse.<Void>ok("Nếu tài khoản tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest r) {
        resets.reset(r.token(), r.newPassword(), r.confirmPassword());
        return ApiResponse.<Void>ok("Đã đặt lại mật khẩu. Vui lòng đăng nhập lại.", null);
    }

    @PostMapping({"/otp/send", "/otp/resend"})
    public ApiResponse<Void> sendOtp(@Valid @RequestBody OtpSendRequest request, HttpServletRequest httpRequest) {
        String message = otps.send(request, httpRequest.getRemoteAddr());
        return ApiResponse.ok(message, null);
    }

    @PostMapping("/otp/verify")
    public ApiResponse<OtpVerificationResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        OtpVerificationResponse result = otps.verify(request);
        return ApiResponse.ok(
                request.purpose() == OtpPurpose.RESET_PASSWORD
                        ? "Mã xác minh hợp lệ. Bạn có thể đặt lại mật khẩu."
                        : "Xác minh thành công.",
                result);
    }
}
