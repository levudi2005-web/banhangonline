package com.banhangonline;

import com.banhangonline.chat.dto.ChatMessageView;
import com.banhangonline.chat.service.ChatService;
import com.banhangonline.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"null", "unchecked"})
class ChatServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ChatService chats = new ChatService(jdbc);

    @Test
    void customerCanSendMessageAndMarkItRead() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                eq(91L), eq(true), eq(22L), eq(false), eq(22L))).thenReturn(1);
        var saved = new ChatMessageView(108L, 22L, "Khách hàng", "Xin chào",
                "2026-10-12T03:00:00");
        doAnswer(invocation -> {
            KeyHolder keys = invocation.getArgument(1);
            keys.getKeyList().add(Map.of("GENERATED_KEY", 108L));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        doReturn(saved).when(jdbc).queryForObject(
                contains("SELECT m.id"), any(RowMapper.class), eq(108L));

        var response = chats.send(91L, 22L, Set.of("CUSTOMER"), "  Xin chào  ");

        assertThat(response).isEqualTo(saved);
        verify(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        verify(jdbc).update(contains("UPDATE chat_conversations"), eq(91L));
        verify(jdbc).update(contains("UPDATE chat_participants"), eq(91L), eq(22L));
    }

    @Test
    void customerCannotReadAnotherCustomersConversation() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                eq(91L), eq(true), eq(55L), eq(false), eq(55L))).thenReturn(0);

        assertThatThrownBy(() -> chats.messages(91L, 55L, Set.of("CUSTOMER")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CHAT_NOT_FOUND");
        verify(jdbc, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void ownerCannotReadConversationsForAStoreTheyDoNotOwn() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(7L), eq(4L))).thenReturn(0);

        assertThatThrownBy(() -> chats.ownerConversations(4L, 7L))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STORE_NOT_FOUND");
        verify(jdbc, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void blankMessageIsRejectedWithoutDatabaseAccess() {
        assertThatThrownBy(() -> chats.send(91L, 22L, Set.of("CUSTOMER"), " \n "))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(jdbc);
    }
}
