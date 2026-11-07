package com.toystorage.backend.services.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.dto.response.products.ProductActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nhật ký quản lý sản phẩm: ghi người thao tác và dữ liệu trước/sau.
 * Dùng bảng activity_logs đã tồn tại; không tạo yêu cầu phê duyệt giả.
 */
@Service
@RequiredArgsConstructor
public class ProductAuditService {
    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> snapshot(Products product) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("productCode", product.getProductsCode());
        values.put("barcode", product.getBarcode());
        values.put("name", product.getName());
        values.put("imageUrl", product.getImageUrl());
        values.put("categoryId", product.getCategory().getId());
        values.put("categoryName", product.getCategory().getName());
        values.put("brandId", product.getBrand() == null ? null : product.getBrand().getId());
        values.put("baseUnit", product.getBaseUnit());
        values.put("purchasePrice", product.getPurchasePrice());
        values.put("sellingPrice", product.getSellingPrice());
        values.put("status", product.getStatus().name());
        return values;
    }

    public void logCreate(Products product, Users actor, String note) {
        save(ActivityAction.CREATE, product.getId(), actor, null, snapshot(product), note);
    }

    public void logUpdate(Products product, Users actor, Map<String, Object> before, String note) {
        save(ActivityAction.UPDATE, product.getId(), actor, before, snapshot(product), note);
    }

    public Page<ProductActivityResponse> getHistory(Long productId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new com.toystorage.backend.exceptions.BadRequest("Số trang hoặc kích thước trang không hợp lệ");
        }
        return activityLogRepository.findByEntityTypeAndEntityId(
                        ActivityEntityType.PRODUCT,
                        productId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(log -> ProductActivityResponse.builder()
                        .id(log.getId())
                        .action(log.getAction().name())
                        .changedByName(log.getUser() == null ? "Hệ thống" : log.getUser().getName())
                        .createdAt(log.getCreatedAt())
                        .oldValue(log.getOldValue())
                        .newValue(log.getNewValue())
                        .build());
    }

    private void save(ActivityAction action, Long entityId, Users actor,
                      Map<String, Object> before, Map<String, Object> after, String note) {
        Map<String, Object> next = new LinkedHashMap<>(after);
        if (note != null && !note.isBlank()) next.put("changeNote", note.trim());
        ActivityLogs entry = ActivityLogs.builder()
                .user(actor)
                .action(action)
                .entityType(ActivityEntityType.PRODUCT)
                .entityId(entityId)
                .oldValue(toJson(before))
                .newValue(toJson(next))
                .build();
        // Cùng transaction với ProductService: thất bại ghi log => rollback cập nhật.
        activityLogRepository.save(entry);
    }

    private String toJson(Map<String, Object> value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể ghi nhật ký thay đổi sản phẩm", e);
        }
    }
}
