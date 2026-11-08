package com.toystorage.backend.dto.response.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/** Thông tin hiển thị lịch sử thay đổi một sản phẩm. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductActivityResponse {
    private Long id;
    private String action;
    private String changedByName;
    private LocalDateTime createdAt;
    private String oldValue;
    private String newValue;
}
