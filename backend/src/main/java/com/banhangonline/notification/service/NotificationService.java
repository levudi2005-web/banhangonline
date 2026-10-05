package com.banhangonline.notification.service;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.notification.dto.NotificationView;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list(Long userId) {
        return jdbc.query("""
                SELECT id,type,title,message,reference_type,reference_id,is_read,created_at,read_at
                FROM notifications WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 100
                """, (rs, row) -> new NotificationView(rs.getLong("id"), rs.getString("type"),
                rs.getString("title"), rs.getString("message"), rs.getString("reference_type"),
                rs.getObject("reference_id", Long.class), rs.getBoolean("is_read"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()), userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        int changed = jdbc.update("""
                UPDATE notifications SET is_read=1,read_at=NOW(6)
                WHERE id=? AND user_id=? AND is_read=0
                """, notificationId, userId);
        if (changed == 0) {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE id=? AND user_id=?",
                    Integer.class, notificationId, userId);
            if (exists == null || exists == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo");
            }
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return jdbc.update("UPDATE notifications SET is_read=1,read_at=NOW(6) WHERE user_id=? AND is_read=0", userId);
    }
}
