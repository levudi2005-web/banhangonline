package com.banhangonline.auth.service;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PasswordRecoveryService {
    private final AuthService auth;
    private final PasswordResetService resets;

    public PasswordRecoveryService(AuthService auth, PasswordResetService resets) {
        this.auth = auth;
        this.resets = resets;
    }

    public String verifyPhoneAndIssueToken(String account, String phoneLastFour) {
        User user = auth.findByAccount(account).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || !Rules.phoneLastFourMatches(user.getPhone(), phoneLastFour)) {
            throw invalidVerification();
        }
        return resets.issueAfterPhoneVerification(user);
    }

    private ApiException invalidVerification() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECOVERY",
                "Tài khoản hoặc thông tin xác minh không hợp lệ.");
    }
}
