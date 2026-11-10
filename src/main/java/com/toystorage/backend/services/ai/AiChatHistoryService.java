
package com.toystorage.backend.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.toystorage.backend.config.security.CustomUserDetails;

import com.toystorage.backend.dto.request.ai.AiChatRequest;
import com.toystorage.backend.dto.response.ai.AiChatResponse;
import com.toystorage.backend.dto.response.ai.AiChatConversationSummaryResponse;
import com.toystorage.backend.dto.response.ai.AiChatConversationDetailResponse;
import com.toystorage.backend.dto.response.ai.AiChatMessageResponse;

import com.toystorage.backend.entity.ai.AiChatConversation;
import com.toystorage.backend.entity.ai.AiChatMessage;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.ai.AiChatConversationRepository;
import com.toystorage.backend.repository.ai.AiChatMessageRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

 @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-BIZ-STAFF', 'ROLE_BIZ_MANAGER', 'ROLE_BIZ_STAFF')")

public class AiChatHistoryService {

    private final AiChatConversationRepository conversationRepository;

    private final AiChatMessageRepository messageRepository;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    /**
     * Lưu một lượt trò chuyện gồm:
     * - Câu hỏi của người dùng.
     * - Câu trả lời của AI hoặc Fallback.
     *
     * conversationId = null:
     * Tạo cuộc hội thoại mới.
     *
     * conversationId có giá trị:
     * Tiếp tục hội thoại thuộc tài khoản hiện tại.
     */
    @Transactional
    public Long saveExchange(
            Long conversationId,
            AiChatRequest request,
            AiChatResponse response
    ) {

        // Kiểm tra câu hỏi hợp lệ.
        if (request == null
                || request.getMessage() == null
                || request.getMessage().isBlank()) {

            throw new BadRequest(
                    "AI question must not be blank."
            );
        }

        // Kiểm tra câu trả lời trước khi lưu.
        if (response == null
                || response.getAnswer() == null
                || response.getAnswer().isBlank()) {

            throw new IllegalStateException(
                    "AI response is missing."
            );
        }

        // Lấy ID người dùng từ phiên đăng nhập.
        Long userId = getCurrentUserId();

        LocalDateTime now = LocalDateTime.now();

        AiChatConversation conversation;

        if (conversationId == null) {

            // Tạo cuộc hội thoại mới.
            Users user = userRepository.getReferenceById(userId);

            conversation = AiChatConversation.builder()
                    .user(user)
                    .title(
                            createConversationTitle(
                                    request.getMessage()
                            )
                    )
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            conversation = conversationRepository.save(
                    conversation
            );

        } else {

            // Chỉ tiếp tục hội thoại của chính người dùng.
            conversation = conversationRepository
                    .findByIdAndUser_Id(
                            conversationId,
                            userId
                    )
                    .orElseThrow(() ->
                            new NotFound(
                                    "Conversation not found."
                            )
                    );

            conversation.setUpdatedAt(now);
        }

        // Gán ID trước khi tạo JSON lưu lịch sử.
        // Nhờ đó response_json cũng chứa conversationId.
        response.setConversationId(conversation.getId());

        // Lưu câu hỏi của người dùng.
        AiChatMessage userMessage = AiChatMessage.builder()
                .conversation(conversation)
                .sender("USER")
                .content(request.getMessage())
                .intent(response.getIntent())
                .createdAt(now)
                .build();

        messageRepository.save(userMessage);

        // Lưu câu trả lời của AI hoặc Fallback.
        // responseJson chứa dữ liệu phân tích, bảng và biểu đồ.
        AiChatMessage assistantMessage = AiChatMessage.builder()
                .conversation(conversation)
                .sender("ASSISTANT")
                .content(response.getAnswer())
                .intent(response.getIntent())
                .aiGenerated(response.getAiGenerated())
                .model(response.getModel())
                .responseJson(
                        objectMapper.valueToTree(response)
                )
                .createdAt(LocalDateTime.now())
                .build();

        messageRepository.save(assistantMessage);

        // Cập nhật thời gian hoạt động mới nhất.
        conversation.setUpdatedAt(LocalDateTime.now());

        conversationRepository.save(conversation);

        // Trả ID để Frontend tiếp tục hội thoại.
        return conversation.getId();
    }

    /**
     * Kiểm tra quyền sở hữu trước khi chat tiếp.
     *
     * Không cho phép truy cập hội thoại của người khác.
     */
    @Transactional(readOnly = true)
    public void verifyOwnership(Long conversationId) {

        // null nghĩa là người dùng muốn tạo cuộc hội thoại mới.
        if (conversationId == null) {
            return;
        }

        if (conversationId <= 0) {
            throw new BadRequest(
                    "Invalid conversation ID."
            );
        }

        Long userId = getCurrentUserId();

        conversationRepository
                .findByIdAndUser_Id(
                        conversationId,
                        userId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Conversation not found."
                        )
                );
    }

    /**
     * Lấy danh sách lịch sử hội thoại.
     *
     * Chỉ lấy hội thoại thuộc tài khoản đăng nhập.
     *
     * page bắt đầu từ 0.
     * size tối đa 50.
     */
    @Transactional(readOnly = true)
    public Page<AiChatConversationSummaryResponse> getMyConversations(
            int page,
            int size
    ) {

        // Kiểm tra tham số phân trang.
        if (page < 0 || size < 1 || size > 50) {
            throw new BadRequest(
                    "Invalid pagination parameters."
            );
        }

        Long userId = getCurrentUserId();

        // Lấy danh sách hội thoại theo người sở hữu.
        // Sắp xếp theo lần cập nhật gần nhất.
        return conversationRepository
                .findByUser_IdOrderByUpdatedAtDescIdDesc(
                        userId,
                        PageRequest.of(page, size)
                )
                .map(conversation ->
                        new AiChatConversationSummaryResponse(
                                conversation.getId(),
                                conversation.getTitle(),
                                conversation.getCreatedAt(),
                                conversation.getUpdatedAt()
                        )
                );
    }

    /**
     * Lấy chi tiết cuộc hội thoại.
     *
     * Bao gồm:
     * - ID cuộc hội thoại.
     * - Tiêu đề.
     * - Thời gian tạo và cập nhật.
     * - Danh sách tin nhắn USER và ASSISTANT.
     *
     * Chỉ chủ sở hữu mới được xem.
     */
    @Transactional(readOnly = true)
    public AiChatConversationDetailResponse getMyConversation(
            Long conversationId
    ) {

        if (conversationId == null || conversationId <= 0) {
            throw new BadRequest(
                    "Invalid conversation ID."
            );
        }

        // Lấy ID người dùng đang đăng nhập.
        Long userId = getCurrentUserId();

        // Kiểm tra quyền sở hữu hội thoại.
        AiChatConversation conversation = conversationRepository
                .findByIdAndUser_Id(
                        conversationId,
                        userId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Conversation not found."
                        )
                );

        // Lấy tin nhắn từ cũ đến mới.
        List<AiChatMessageResponse> messages = messageRepository
                .findByConversation_IdAndConversation_User_IdOrderByCreatedAtAscIdAsc(
                        conversationId,
                        userId
                )
                .stream()
                .map(message ->
                        new AiChatMessageResponse(
                                message.getId(),
                                message.getSender(),
                                message.getContent(),
                                message.getIntent(),
                                message.getAiGenerated(),
                                message.getModel(),
                                message.getResponseJson(),
                                message.getCreatedAt()
                        )
                )
                .toList();

        // Trả thông tin cuộc hội thoại và tin nhắn.
        return new AiChatConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    /**
     * Lấy ID người dùng đang đăng nhập
     * thông qua Spring Security.
     *
     * Không lấy userId từ request của Frontend.
     */
    private Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                        instanceof CustomUserDetails)) {

            throw new Unauthorized(
                    "Authentication is required."
            );
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return userDetails.getId();
    }

    /**
     * Tự động tạo tiêu đề từ câu hỏi đầu tiên.
     *
     * Giới hạn 100 ký tự để phù hợp
     * với cột title VARCHAR(200).
     */
    private String createConversationTitle(String question) {

        String title = question.strip();

        if (title.length() > 100) {
            return title.substring(0, 100);
        }

        return title;
    }
}
