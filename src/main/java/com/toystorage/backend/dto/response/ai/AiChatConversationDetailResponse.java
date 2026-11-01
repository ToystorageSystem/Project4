package com.toystorage.backend.dto.response.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu chi tiết cuộc hội thoại.
 * Bao gồm thông tin cuộc hội thoại và các tin nhắn.
 */
public record AiChatConversationDetailResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AiChatMessageResponse> messages
) {
}
