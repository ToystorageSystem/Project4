
package com.toystorage.backend.dto.response.ai;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * Dữ liệu một tin nhắn trong lịch sử hội thoại.
 */
public record AiChatMessageResponse(
        Long id,
        String sender,
        String content,
        String intent,
        Boolean aiGenerated,
        String model,
        JsonNode responseJson,
        LocalDateTime createdAt
) {
}
