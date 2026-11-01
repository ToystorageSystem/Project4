
package com.toystorage.backend.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    /**
     * ID của cuộc hội thoại AI.
     *
     * null:
     * Cuộc hội thoại chưa được lưu.
     *
     * Có ID:
     * Cuộc hội thoại đã được lưu vào database.
     *
     * Frontend sử dụng ID này để tiếp tục
     * cuộc hội thoại hiện tại.
     */
    private Long conversationId;

    /**
     * Câu hỏi gốc của người dùng.
     */
    private String question;

    /**
     * Intent mà backend xác định.
     *
     * Ví dụ:
     *
     * INVENTORY
     * SALES
     * PRODUCT
     * PURCHASE_ORDER
     * SUPPLIER
     * STOCK_TRANSFER
     * FORECAST
     * GENERAL
     */
    private String intent;

    /**
     * Câu trả lời tự nhiên từ AI
     * hoặc từ Fallback Service.
     */
    private String answer;

    /**
     * Xác định AI provider có thực sự
     * được gọi để tạo câu trả lời hay không.
     *
     * true:
     * Câu trả lời được tạo bởi AI provider.
     *
     * false:
     * Hệ thống sử dụng Fallback Answer.
     */
    private Boolean aiGenerated;

    /**
     * Tên model AI đã sử dụng.
     *
     * null nếu AI chưa được bật
     * hoặc không có model được ghi nhận.
     */
    private String model;

    /**
     * Dữ liệu analytics mà backend
     * đã cung cấp cho AI.
     *
     * Giữ kiểu Object để hỗ trợ
     * nhiều loại dữ liệu theo intent.
     */
    private Object analyticsData;

    /**
     * Dữ liệu bảng dành cho Frontend.
     *
     * Mỗi phần tử đại diện cho một dòng.
     *
     * Ví dụ:
     * Map<String, Object>
     */
    private List<Map<String, Object>> tableData;

    /**
     * Metadata giúp Frontend vẽ biểu đồ.
     *
     * Ví dụ:
     *
     * {
     *   "type": "BAR",
     *   "title": "Inventory Movement",
     *   "dataSource": "inventoryMovement",
     *   "xAxis": "productName",
     *   "yAxis": "netMovement"
     * }
     */
    private Map<String, Object> chartMetadata;

    /**
     * Thời điểm backend tạo response.
     */
    private LocalDateTime generatedAt;
}
