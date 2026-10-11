package com.banhangonline.chat.dto;

public record ChatConversationView(
        Long id,
        Long storeId,
        String storeName,
        String customerName,
        String lastMessage,
        String lastMessageAt,
        int unreadCount
) {}
