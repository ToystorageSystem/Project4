package com.toystorage.backend.dto.request.products;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateProductRequest {

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(
            max = 50,
            message = "Mã sản phẩm không được vượt quá 50 ký tự"
    )
    private String productCode;

    @NotBlank(message = "Barcode không được để trống")
    @Size(
            max = 100,
            message = "Barcode không được vượt quá 100 ký tự"
    )
    private String barcode;

    @Size(
            max = 500,
            message = "Đường dẫn hình ảnh không được vượt quá 500 ký tự"
    )
    private String imageUrl;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(
            max = 200,
            message = "Tên sản phẩm không được vượt quá 200 ký tự"
    )
    private String name;

    @NotNull(message = "Danh mục không được để trống")
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private Long categoryId;

    @Positive(message = "ID thương hiệu phải lớn hơn 0")
    private Long brandId;

    @NotBlank(message = "Đơn vị cơ sở không được để trống")
    @Size(
            max = 30,
            message = "Đơn vị cơ sở không được vượt quá 30 ký tự"
    )
    private String baseUnit;

    @NotNull(message = "Giá nhập không được để trống")
    @DecimalMin(
            value = "0.01",
            message = "Giá nhập phải lớn hơn 0"
    )
    private BigDecimal purchasePrice;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(
            value = "0.01",
            message = "Giá bán phải lớn hơn 0"
    )
    private BigDecimal sellingPrice;

    @Size(
            max = 500,
            message = "Lý do yêu cầu không được vượt quá 500 ký tự"
    )
    private String requestReason;
}