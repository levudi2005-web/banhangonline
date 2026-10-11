package com.banhangonline.chat.service;

import com.banhangonline.chat.dto.ChatConversationView;
import com.banhangonline.chat.dto.ChatMessageView;
import com.banhangonline.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class ChatService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final JdbcTemplate jdbc;

    public ChatService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public ChatConversationView startOrGet(Long customerId, Long storeId, Long orderId) {
        var stores = jdbc.query(
                "SELECT id, name, owner_user_id FROM stores WHERE id=? AND status='ACTIVE'",
                (rs, row) -> new StoreData(rs.getLong("id"), rs.getString("name"), rs.getLong("owner_user_id")),
                storeId);
        if (stores.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng đang hoạt động");
        }
        StoreData store = stores.get(0);
        if (orderId != null) {
            Integer ownedOrder = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM orders WHERE id=? AND user_id=? AND store_id=?",
                    Integer.class, orderId, customerId, storeId);
            if (ownedOrder == null || ownedOrder == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng của bạn");
            }
        }

        List<Long> existing = jdbc.query("""
                SELECT c.id FROM chat_conversations c
                JOIN chat_participants p ON p.conversation_id=c.id
                WHERE c.store_id=? AND c.status='OPEN' AND p.user_id=? AND p.participant_role='CUSTOMER'
                  AND p.left_at IS NULL
                ORDER BY c.id DESC LIMIT 1
                """, (rs, row) -> rs.getLong(1), storeId, customerId);
        Long conversationId;
        if (!existing.isEmpty()) {
            conversationId = existing.get(0);
            if (orderId != null) {
                jdbc.update("UPDATE chat_conversations SET order_id=COALESCE(order_id, ?) WHERE id=?",
                        orderId, conversationId);
            }
        } else {
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO chat_conversations (store_id, order_id, created_by_user_id, status)
                        VALUES (?, ?, ?, 'OPEN')
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, storeId);
                if (orderId == null) statement.setNull(2, java.sql.Types.BIGINT);
                else statement.setLong(2, orderId);
                statement.setLong(3, customerId);
                return statement;
            }, key);
            Number generatedId = key.getKey();
            if (generatedId == null) {
                throw new IllegalStateException("The database did not return the new chat conversation ID.");
            }
            conversationId = generatedId.longValue();
            jdbc.update("""
                    INSERT INTO chat_participants (conversation_id, user_id, participant_role, last_read_at)
                    VALUES (?, ?, 'CUSTOMER', CURRENT_TIMESTAMP(6)), (?, ?, 'OWNER', CURRENT_TIMESTAMP(6))
                    """, conversationId, customerId, conversationId, store.ownerId());
        }
        return conversation(conversationId, customerId);
    }

    public List<ChatConversationView> customerConversations(Long userId) {
        return jdbc.query("""
                SELECT c.id, c.store_id, s.name AS store_name, u.full_name AS customer_name,
                  (SELECT m.message_text FROM chat_messages m WHERE m.conversation_id=c.id
                   ORDER BY m.id DESC LIMIT 1) AS last_message,
                  (SELECT m.created_at FROM chat_messages m WHERE m.conversation_id=c.id
                   ORDER BY m.id DESC LIMIT 1) AS last_message_at,
                  (SELECT COUNT(*) FROM chat_messages m
                   JOIN chat_participants rp ON rp.conversation_id=m.conversation_id AND rp.user_id=?
                   WHERE m.conversation_id=c.id AND m.sender_user_id<>? AND m.created_at>rp.last_read_at) AS unread_count
                FROM chat_conversations c
                JOIN stores s ON s.id=c.store_id
                JOIN users u ON u.id=c.created_by_user_id
                JOIN chat_participants p ON p.conversation_id=c.id AND p.user_id=? AND p.participant_role='CUSTOMER'
                WHERE c.status='OPEN' AND p.left_at IS NULL
                ORDER BY COALESCE(c.updated_at,c.created_at) DESC, c.id DESC
                """, this::mapConversation, userId, userId, userId);
    }

    public List<ChatConversationView> ownerConversations(Long ownerId, Long storeId) {
        requireStoreOwner(storeId, ownerId);
        return jdbc.query("""
                SELECT c.id, c.store_id, s.name AS store_name, u.full_name AS customer_name,
                  (SELECT m.message_text FROM chat_messages m WHERE m.conversation_id=c.id
                   ORDER BY m.id DESC LIMIT 1) AS last_message,
                  (SELECT m.created_at FROM chat_messages m WHERE m.conversation_id=c.id
                   ORDER BY m.id DESC LIMIT 1) AS last_message_at,
                  (SELECT COUNT(*) FROM chat_messages m
                   JOIN chat_participants rp ON rp.conversation_id=m.conversation_id AND rp.user_id=?
                   WHERE m.conversation_id=c.id AND m.sender_user_id<>? AND m.created_at>rp.last_read_at) AS unread_count
                FROM chat_conversations c
                JOIN stores s ON s.id=c.store_id
                JOIN users u ON u.id=c.created_by_user_id
                JOIN chat_participants p ON p.conversation_id=c.id AND p.participant_role='CUSTOMER'
                WHERE c.store_id=? AND c.status='OPEN' AND p.left_at IS NULL
                ORDER BY COALESCE(c.updated_at,c.created_at) DESC, c.id DESC
                """, this::mapConversation, ownerId, ownerId, storeId);
    }

    public List<ChatMessageView> messages(Long conversationId, Long userId, Set<String> roles) {
        requireConversationAccess(conversationId, userId, roles);
        return jdbc.query("""
                SELECT m.id, m.sender_user_id, u.full_name AS sender_name, m.message_text, m.created_at
                FROM chat_messages m JOIN users u ON u.id=m.sender_user_id
                WHERE m.conversation_id=?
                ORDER BY m.id DESC LIMIT 200
                """, (rs, row) -> new ChatMessageView(
                rs.getLong("id"), rs.getLong("sender_user_id"), rs.getString("sender_name"),
                rs.getString("message_text"), format(rs.getTimestamp("created_at"))), conversationId)
                .reversed();
    }

    @Transactional
    public ChatMessageView send(Long conversationId, Long senderId, Set<String> roles, String messageText) {
        String text = messageText == null ? "" : messageText.trim();
        if (text.isEmpty() || text.length() > 4000) {
            throw ApiException.validation("Tin nhắn phải có từ 1 đến 4.000 ký tự");
        }
        requireConversationAccess(conversationId, senderId, roles);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO chat_messages (conversation_id, sender_user_id, message_text)
                    VALUES (?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, conversationId);
            statement.setLong(2, senderId);
            statement.setString(3, text);
            return statement;
        }, key);
        jdbc.update("UPDATE chat_conversations SET updated_at=CURRENT_TIMESTAMP(6) WHERE id=?", conversationId);
        markRead(conversationId, senderId);
        Number generatedId = key.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return the new chat message ID.");
        }
        Long messageId = generatedId.longValue();
        return jdbc.queryForObject("""
                SELECT m.id, m.sender_user_id, u.full_name AS sender_name, m.message_text, m.created_at
                FROM chat_messages m JOIN users u ON u.id=m.sender_user_id WHERE m.id=?
                """, (rs, row) -> new ChatMessageView(
                rs.getLong("id"), rs.getLong("sender_user_id"), rs.getString("sender_name"),
                rs.getString("message_text"), format(rs.getTimestamp("created_at"))), messageId);
    }

    @Transactional
    public void markRead(Long conversationId, Long userId, Set<String> roles) {
        requireConversationAccess(conversationId, userId, roles);
        markRead(conversationId, userId);
    }

    private void markRead(Long conversationId, Long userId) {
        jdbc.update("""
                UPDATE chat_participants SET last_read_at=CURRENT_TIMESTAMP(6)
                WHERE conversation_id=? AND user_id=? AND left_at IS NULL
                """, conversationId, userId);
    }

    private ChatConversationView conversation(Long conversationId, Long userId) {
        return jdbc.queryForObject("""
                SELECT c.id, c.store_id, s.name AS store_name, u.full_name AS customer_name,
                  (SELECT m.message_text FROM chat_messages m WHERE m.conversation_id=c.id
                   ORDER BY m.id DESC LIMIT 1) AS last_message,
                  (SELECT m.created_at FROM chat_messages m WHERE m.conversation_id=c.id
                   ORDER BY m.id DESC LIMIT 1) AS last_message_at,
                  (SELECT COUNT(*) FROM chat_messages m
                   JOIN chat_participants rp ON rp.conversation_id=m.conversation_id AND rp.user_id=?
                   WHERE m.conversation_id=c.id AND m.sender_user_id<>? AND m.created_at>rp.last_read_at) AS unread_count
                FROM chat_conversations c JOIN stores s ON s.id=c.store_id
                JOIN users u ON u.id=c.created_by_user_id WHERE c.id=?
                """, this::mapConversation, userId, userId, conversationId);
    }

    private ChatConversationView mapConversation(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ChatConversationView(rs.getLong("id"), rs.getLong("store_id"), rs.getString("store_name"),
                rs.getString("customer_name"), rs.getString("last_message"),
                format(rs.getTimestamp("last_message_at")), rs.getInt("unread_count"));
    }

    private void requireConversationAccess(Long conversationId, Long userId, Set<String> roles) {
        Integer allowed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM chat_conversations c
                WHERE c.id=? AND (
                  (? AND EXISTS (SELECT 1 FROM chat_participants p WHERE p.conversation_id=c.id
                    AND p.user_id=? AND p.participant_role='CUSTOMER' AND p.left_at IS NULL))
                  OR (? AND EXISTS (SELECT 1 FROM stores s WHERE s.id=c.store_id AND s.owner_user_id=?))
                )
                """, Integer.class, conversationId, roles.contains("CUSTOMER"), userId,
                roles.contains("OWNER"), userId);
        if (allowed == null || allowed == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND", "Không tìm thấy cuộc trò chuyện");
        }
    }

    private void requireStoreOwner(Long storeId, Long ownerId) {
        Integer allowed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stores WHERE id=? AND owner_user_id=?",
                Integer.class, storeId, ownerId);
        if (allowed == null || allowed == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng");
        }
    }

    private String format(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime().format(DATE_TIME);
    }

    private record StoreData(Long id, String name, Long ownerId) {}
}
