
package com.toystorage.backend.controllers.ai;

import com.toystorage.backend.dto.request.ai.AiChatRequest;
import com.toystorage.backend.dto.response.ai.AiChatResponse;
import com.toystorage.backend.services.ai.AiChatService;
import com.toystorage.backend.services.ai.AiChatHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BIZ_MANAGER', 'BIZ_STAFF')")
public class AiChatController {

    private final AiChatService aiChatService;

    private final AiChatHistoryService aiChatHistoryService;

    /**
     * Nhận câu hỏi, phân tích và lưu lịch sử AI Chat.
     *
     * Chỉ Business Manager và Business Staff được sử dụng.
     *
     * Nếu conversationId = null:
     * - Tạo một cuộc hội thoại mới.
     *
     * Nếu conversationId có giá trị:
     * - Kiểm tra quyền sở hữu.
     * - Tiếp tục cuộc hội thoại đã có.
     *
     * API: POST /api/analytics/ai/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request
    ) {

        // Bước 1: Kiểm tra quyền sở hữu hội thoại.
        // Phải kiểm tra trước khi thực hiện phân tích AI.
        aiChatHistoryService.verifyOwnership(
                request.getConversationId()
        );

        // Bước 2: Phân tích dữ liệu và tạo câu trả lời.
        AiChatResponse response = aiChatService.chat(request);

        // Bước 3: Lưu câu hỏi và câu trả lời vào database.
        Long conversationId = aiChatHistoryService.saveExchange(
                request.getConversationId(),
                request,
                response
        );

        // Bước 4: Gửi ID cuộc hội thoại cho Frontend.
        response.setConversationId(conversationId);

        // Bước 5: Trả kết quả phân tích cho người dùng.
        return ResponseEntity.ok(response);
    }
}
