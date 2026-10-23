package com.toystorage.backend.dto.response.suppliers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProductResponse {

    /**
     * ID của liên kết supplier_products.
     */
    private Long id;

    /**
     * Mã liên kết supplier_products.
     */
    private String linkCode;

    /**
     * Thông tin sản phẩm.
     */
    private Long productId;

    private String productCode;

    private String productName;

    private String productStatus;

    /**
     * Thông tin nhà cung cấp.
     */
    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private String supplierStatus;

    /**
     * Mã sản phẩm do nhà cung cấp sử dụng.
     */
    private String supplierProductCode;

    /**
     * Giá nhập từ nhà cung cấp này.
     */
    private BigDecimal purchasePrice;

    /**
     * Thời gian giao hàng dự kiến.
     */
    private Integer leadTimeDays;

    /**
     * Số lượng đặt hàng tối thiểu.
     */
    private Integer minimumOrderQuantity;

    /**
     * Có phải nhà cung cấp mặc định
     * của sản phẩm hay không.
     */
    private Boolean defaultSupplier;

    /**
     * Trạng thái của liên kết.
     */
    private String status;
}