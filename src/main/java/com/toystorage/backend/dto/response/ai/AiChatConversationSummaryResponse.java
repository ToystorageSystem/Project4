
package com.toystorage.backend.dto.response.ai;

import java.time.LocalDateTime;

/**
 * Dữ liệu tóm tắt của một cuộc hội thoại.
 * Được sử dụng khi hiển thị danh sách lịch sử AI Chat.
 */
public record AiChatConversationSummaryResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
