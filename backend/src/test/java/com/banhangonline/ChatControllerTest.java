package com.banhangonline;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.chat.controller.ChatController;
import com.banhangonline.chat.dto.ChatConversationView;
import com.banhangonline.chat.dto.ChatStartRequest;
import com.banhangonline.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatControllerTest {
    private final ChatService chats = mock(ChatService.class);
    private final ChatController controller = new ChatController(chats);

    @Test
    void customerStartsConversationAndReceivesPersistedConversationView() {
        var customer = new AuthPrincipal(22L, "customer", Set.of("CUSTOMER"));
        var conversation = new ChatConversationView(91L, 7L, "Cửa hàng", "Khách hàng",
                null, null, 0);
        when(chats.startOrGet(22L, 7L, null)).thenReturn(conversation);

        var response = controller.start(customer, new ChatStartRequest(7L, null));

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(conversation);
        verify(chats).startOrGet(22L, 7L, null);
    }

    @Test
    void ownerCanListStoreConversationsButCustomerCannot() {
        var owner = new AuthPrincipal(4L, "owner", Set.of("OWNER"));
        var conversation = new ChatConversationView(91L, 7L, "Cửa hàng", "Khách hàng",
                "Xin chào", "2026-10-12T03:00:00", 1);
        when(chats.ownerConversations(4L, 7L)).thenReturn(List.of(conversation));

        var response = controller.ownerConversations(owner, 7L);

        assertThat(response.data()).containsExactly(conversation);
        assertThatThrownBy(() -> controller.ownerConversations(
                new AuthPrincipal(22L, "customer", Set.of("CUSTOMER")), 7L))
                .isInstanceOf(AccessDeniedException.class);
        verify(chats).ownerConversations(4L, 7L);
        verifyNoMoreInteractions(chats);
    }

    @Test
    void nonParticipantsCannotReadOrSendMessages() {
        var staff = new AuthPrincipal(31L, "staff", Set.of("STAFF"));

        assertThatThrownBy(() -> controller.messages(staff, 91L))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.send(staff, 91L,
                new com.banhangonline.chat.dto.ChatMessageRequest("Hello")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(chats);
    }
}
