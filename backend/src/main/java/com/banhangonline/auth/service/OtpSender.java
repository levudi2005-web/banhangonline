package com.banhangonline.auth.service;

import com.banhangonline.auth.entity.OtpChannel;

public interface OtpSender {
    OtpChannel channel();

    void ensureConfigured();

    void send(String destination, String code, int expirationSeconds);
}
