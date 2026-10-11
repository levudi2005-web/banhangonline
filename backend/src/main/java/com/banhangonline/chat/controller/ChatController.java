package com.banhangonline.chat.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.chat.dto.ChatConversationView;
import com.banhangonline.chat.dto.ChatMessageRequest;
import com.banhangonline.chat.dto.ChatMessageView;
import com.banhangonline.chat.dto.ChatStartRequest;
import com.banhangonline.chat.service.ChatService;
import com.banhangonline.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ChatController {
    private final ChatService chats;

    public ChatController(ChatService chats) {
        this.chats = chats;
    }

    @GetMapping("/api/customer/chat/conversations")
    public ApiResponse<List<ChatConversationView>> customerConversations(
            @AuthenticationPrincipal AuthPrincipal principal) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Cuộc trò chuyện của bạn", chats.customerConversations(principal.userId()));
    }

    @PostMapping("/api/customer/chat/conversations")
    public ApiResponse<ChatConversationView> start(
            @AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody ChatStartRequest request) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Cuộc trò chuyện với cửa hàng", chats.startOrGet(
                principal.userId(), request.storeId(), request.orderId()));
    }

    @GetMapping("/api/owner/chat/conversations")
    public ApiResponse<List<ChatConversationView>> ownerConversations(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestParam Long storeId) {
        requireRole(principal, "OWNER");
        return ApiResponse.ok("Tin nhắn khách hàng",
                chats.ownerConversations(principal.userId(), storeId));
    }

    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    public ApiResponse<List<ChatMessageView>> messages(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long conversationId) {
        requireParticipantRole(principal);
        return ApiResponse.ok("Lịch sử trò chuyện",
                chats.messages(conversationId, principal.userId(), principal.roles()));
    }

    @PostMapping("/api/chat/conversations/{conversationId}/messages")
    public ApiResponse<ChatMessageView> send(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageRequest request) {
        requireParticipantRole(principal);
        return ApiResponse.ok("Đã gửi tin nhắn",
                chats.send(conversationId, principal.userId(), principal.roles(), request.messageText()));
    }

    @PatchMapping("/api/chat/conversations/{conversationId}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long conversationId) {
        requireParticipantRole(principal);
        chats.markRead(conversationId, principal.userId(), principal.roles());
        return ApiResponse.ok("Đã đánh dấu đã đọc", null);
    }

    private void requireParticipantRole(AuthPrincipal principal) {
        if (principal == null || (!principal.roles().contains("CUSTOMER") && !principal.roles().contains("OWNER"))) {
            throw new AccessDeniedException("CHAT_PARTICIPANT_REQUIRED");
        }
    }

    private void requireRole(AuthPrincipal principal, String role) {
        if (principal == null || !principal.roles().contains(role)) {
            throw new AccessDeniedException(role + "_REQUIRED");
        }
    }
}
