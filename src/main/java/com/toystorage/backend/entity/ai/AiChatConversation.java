
package com.toystorage.backend.entity.ai;

import com.toystorage.backend.entity.users.Users;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatConversation {

    // Khóa chính của cuộc hội thoại.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tài khoản sở hữu cuộc hội thoại.
    // Một người dùng có thể có nhiều cuộc hội thoại AI.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // Tiêu đề cuộc hội thoại.
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // Thời gian tạo cuộc hội thoại.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Thời gian cập nhật cuộc hội thoại gần nhất.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Tự động thiết lập thời gian khi tạo mới.
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    // Cập nhật thời gian khi bản ghi hội thoại được sửa.
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
