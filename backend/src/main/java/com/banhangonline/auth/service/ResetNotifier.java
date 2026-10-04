package com.banhangonline.auth.service;

import com.banhangonline.user.entity.User;

/** Sends password reset links through the configured delivery provider. */
public interface ResetNotifier {
    void ensureConfigured();

    void send(User user, String rawToken);
}
