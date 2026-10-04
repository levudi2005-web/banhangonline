package com.banhangonline.auth.service;

import com.banhangonline.user.entity.User;

/** Điểm nối để gửi link đặt lại mật khẩu (email/SMS). Chưa có nhà cung cấp thì dùng bản mặc định bên dưới. */
public interface ResetNotifier {
    void send(User user, String rawToken);
}
