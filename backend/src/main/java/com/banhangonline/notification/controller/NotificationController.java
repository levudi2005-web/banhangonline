package com.banhangonline.notification.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.notification.dto.NotificationView;
import com.banhangonline.notification.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public ApiResponse<List<NotificationView>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok("Thông báo", notifications.list(principal.userId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal AuthPrincipal principal,
                                      @PathVariable Long notificationId) {
        notifications.markRead(principal.userId(), notificationId);
        return ApiResponse.ok("Đã đánh dấu đã đọc", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Integer> markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok("Đã đánh dấu các thông báo là đã đọc",
                notifications.markAllRead(principal.userId()));
    }
}
