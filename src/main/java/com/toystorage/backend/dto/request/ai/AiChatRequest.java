
package com.toystorage.backend.dto.request.ai;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    // Câu hỏi người dùng gửi đến AI.
    @NotBlank(message = "AI question must not be blank.")
    private String message;

    // ID cuộc hội thoại.
    // null: tạo cuộc hội thoại mới.
    // Có ID: tiếp tục cuộc hội thoại hiện có.
    private Long conversationId;

    // Các bộ lọc dữ liệu dùng cho phân tích.
    private Long storeId;

    private Long warehouseId;

    private Long productId;
}
