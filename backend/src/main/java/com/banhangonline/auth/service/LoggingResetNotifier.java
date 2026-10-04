package com.banhangonline.auth.service;

import com.banhangonline.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Mặc định: chưa gửi gì và tuyệt đối không ghi token ra log. Thay bằng bản gửi email thật sau. */
@Component
public class LoggingResetNotifier implements ResetNotifier {
    private static final Logger log = LoggerFactory.getLogger(LoggingResetNotifier.class);

    @Override
    public void send(User user, String rawToken) {
        log.warn("Có yêu cầu đặt lại mật khẩu cho user id={} nhưng chưa cấu hình nhà cung cấp email", user.getId());
    }
}
