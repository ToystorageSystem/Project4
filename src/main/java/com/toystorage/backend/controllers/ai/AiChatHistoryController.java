
package com.toystorage.backend.controllers.ai;

import com.toystorage.backend.dto.response.ai.AiChatConversationDetailResponse;
import com.toystorage.backend.dto.response.ai.AiChatConversationSummaryResponse;
import com.toystorage.backend.services.ai.AiChatHistoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/ai/conversations")
@RequiredArgsConstructor
 @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-BIZ-STAFF', 'ROLE_BIZ_MANAGER', 'ROLE_BIZ_STAFF')")
public class AiChatHistoryController {

    private final AiChatHistoryService aiChatHistoryService;

    /**
     * Lấy danh sách lịch sử AI Chat của tài khoản hiện tại.
     *
     * Hỗ trợ phân trang:
     * page bắt đầu từ 0.
     * size tối đa 50.
     */
    @GetMapping
    public ResponseEntity<Page<AiChatConversationSummaryResponse>>
    getMyConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        return ResponseEntity.ok(
                aiChatHistoryService.getMyConversations(page, size)
        );
    }

    /**
     * Xem nội dung cuộc hội thoại.
     *
     * Chỉ chủ sở hữu được truy cập.
     * Nếu không sở hữu, trả về 404.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<AiChatConversationDetailResponse>
    getMyConversation(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                aiChatHistoryService.getMyConversation(conversationId)
        );
    }
}
