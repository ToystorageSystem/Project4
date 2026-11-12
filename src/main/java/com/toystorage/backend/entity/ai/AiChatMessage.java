
package com.toystorage.backend.entity.ai;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatMessage {

    // Khóa chính của tin nhắn.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cuộc hội thoại chứa tin nhắn này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiChatConversation conversation;

    // Người gửi tin nhắn: USER hoặc ASSISTANT.
    @Column(name = "sender", nullable = false, length = 20)
    private String sender;

    // Nội dung câu hỏi hoặc câu trả lời.
    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String content;

    // Nhóm phân tích được nhận diện.
    @Column(name = "intent", length = 50)
    private String intent;

    // Xác định câu trả lời có được OpenAI tạo hay không.
    @Column(name = "ai_generated")
    private Boolean aiGenerated;

    // Tên model AI được sử dụng, nếu có.
    @Column(name = "model", length = 120)
    private String model;

    // Lưu bản JSON của kết quả phân tích tại thời điểm chat.
    // Dữ liệu này phục vụ xem lại lịch sử, không phải số liệu realtime.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", columnDefinition = "json")
    private JsonNode responseJson;

    // Thời gian tạo tin nhắn.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tự động thiết lập thời gian khi lưu tin nhắn.
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
