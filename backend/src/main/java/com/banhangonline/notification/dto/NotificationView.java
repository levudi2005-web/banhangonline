package com.banhangonline.notification.dto;

import java.time.Instant;

public record NotificationView(Long id, String type, String title, String message,
                               String referenceType, Long referenceId, boolean read,
                               Instant createdAt, Instant readAt) {}
