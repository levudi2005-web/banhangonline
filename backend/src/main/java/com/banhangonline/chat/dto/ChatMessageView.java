package com.banhangonline.chat.dto;

public record ChatMessageView(
        Long id,
        Long senderUserId,
        String senderName,
        String messageText,
        String createdAt
) {}
