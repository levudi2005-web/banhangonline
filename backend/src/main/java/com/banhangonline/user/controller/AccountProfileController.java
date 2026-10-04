package com.banhangonline.user.controller;

import com.banhangonline.auth.dto.UserResponse;
import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.user.dto.ProfileUpdateRequest;
import com.banhangonline.user.service.AccountProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class AccountProfileController {
    private final AccountProfileService profiles;

    public AccountProfileController(AccountProfileService profiles) {
        this.profiles = profiles;
    }

    @PostMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok("Thông tin tài khoản đã được cập nhật.",
                profiles.update(principal.userId(), request));
    }
}
