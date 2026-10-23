package com.toystorage.backend.dto.response.products;

import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeRequestResponse {

    private Long id;

    private String requestCode;

    private ProductResponse product;

    /**
     * Dữ liệu sản phẩm trước khi thay đổi, dạng JSON.
     * Yêu cầu tạo mới sẽ có giá trị null.
     */
    private String oldValue;

    /**
     * Dữ liệu sản phẩm được đề nghị, dạng JSON.
     */
    private String newValue;

    private String requestReason;

    private String rejectionReason;

    private ProductChangeRequestStatus status;

    private Long createdById;

    private String createdByName;

    private Long approvedById;

    private String approvedByName;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;
}