
package com.toystorage.backend.repository.ai;

import com.toystorage.backend.entity.ai.AiChatMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository
        extends JpaRepository<AiChatMessage, Long> {

    /**
     * Lấy các tin nhắn thuộc một cuộc hội thoại.
     *
     * Kiểm tra cả ID cuộc hội thoại và ID chủ sở hữu
     * để không trả tin nhắn của người dùng khác.
     *
     * Sắp xếp từ tin nhắn cũ đến mới.
     * Nếu trùng thời gian thì tiếp tục sắp xếp theo ID.
     */
    List<AiChatMessage>
    findByConversation_IdAndConversation_User_IdOrderByCreatedAtAscIdAsc(
            Long conversationId,
            Long userId
    );
}
