
package com.toystorage.backend.repository.ai;

import com.toystorage.backend.entity.ai.AiChatConversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatConversationRepository
        extends JpaRepository<AiChatConversation, Long> {

    /**
     * Lấy danh sách cuộc hội thoại của một người dùng.
     *
     * Sắp xếp từ cuộc hội thoại cập nhật gần nhất.
     * Hỗ trợ phân trang để tránh tải quá nhiều dữ liệu.
     */
    Page<AiChatConversation> findByUser_IdOrderByUpdatedAtDescIdDesc(
            Long userId,
            Pageable pageable
    );

    /**
     * Tìm cuộc hội thoại theo ID và người sở hữu.
     *
     * Chỉ trả về khi cuộc hội thoại thuộc đúng userId.
     * Nếu người dùng không sở hữu hội thoại, kết quả rỗng.
     */
    Optional<AiChatConversation> findByIdAndUser_Id(
            Long conversationId,
            Long userId
    );
}
