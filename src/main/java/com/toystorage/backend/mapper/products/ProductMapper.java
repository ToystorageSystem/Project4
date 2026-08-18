package com.toystorage.backend.mapper.products;

import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.request.products.UpdateProductRequest;
import com.toystorage.backend.dto.response.products.ProductChangeRequestResponse;
import com.toystorage.backend.dto.response.products.ProductPageResponse;
import com.toystorage.backend.dto.response.products.ProductResponse;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProductMapper {

    public Products toEntity(
            CreateProductRequest request,
            Categories category,
            Brands brand,
            Users createdBy
    ) {
        return Products.builder()
                .productsCode(
                        request.getProductCode()
                                .trim()
                                .toUpperCase()
                )
                .barcode(request.getBarcode().trim())
                .imageUrl(
                        normalizeNullable(
                                request.getImageUrl()
                        )
                )
                .name(request.getName().trim())
                .category(category)
                .brand(brand)
                .baseUnit(
                        request.getBaseUnit()
                                .trim()
                                .toUpperCase()
                )
                .purchasePrice(
                        request.getPurchasePrice()
                )
                .sellingPrice(
                        request.getSellingPrice()
                )
                .status(ProductStatus.PENDING_CREATE)
                .createdBy(createdBy)
                .build();
    }

    public ProductResponse toResponse(
            Products product
    ) {
        Brands brand = product.getBrand();
        Users approvedBy = product.getApprovedBy();

        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductsCode())
                .barcode(product.getBarcode())
                .imageUrl(product.getImageUrl())
                .name(product.getName())
                .categoryId(
                        product.getCategory().getId()
                )
                .categoryCode(
                        product.getCategory()
                                .getCategoriesCode()
                )
                .categoryName(
                        product.getCategory().getName()
                )
                .brandId(
                        brand != null
                                ? brand.getId()
                                : null
                )
                .brandCode(
                        brand != null
                                ? brand.getBrandsCode()
                                : null
                )
                .brandName(
                        brand != null
                                ? brand.getName()
                                : null
                )
                .baseUnit(product.getBaseUnit())
                .purchasePrice(
                        product.getPurchasePrice()
                )
                .sellingPrice(
                        product.getSellingPrice()
                )
                .status(product.getStatus())
                .createdById(
                        product.getCreatedBy().getId()
                )
                .createdByName(
                        product.getCreatedBy().getName()
                )
                .approvedById(
                        approvedBy != null
                                ? approvedBy.getId()
                                : null
                )
                .approvedByName(
                        approvedBy != null
                                ? approvedBy.getName()
                                : null
                )
                .rejectionReason(
                        product.getRejectionReason()
                )
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductResponse toProposedResponse(
            Products product,
            UpdateProductRequest request,
            Categories category,
            Brands brand
    ) {
        ProductResponse response =
                toResponse(product);

        response.setProductCode(
                request.getProductCode()
                        .trim()
                        .toUpperCase(Locale.ROOT)
        );

        response.setBarcode(
                request.getBarcode().trim()
        );

        response.setImageUrl(
                normalizeNullable(
                        request.getImageUrl()
                )
        );

        response.setName(
                request.getName().trim()
        );

        response.setCategoryId(category.getId());
        response.setCategoryCode(
                category.getCategoriesCode()
        );
        response.setCategoryName(
                category.getName()
        );

        response.setBrandId(
                brand != null
                        ? brand.getId()
                        : null
        );
        response.setBrandCode(
                brand != null
                        ? brand.getBrandsCode()
                        : null
        );
        response.setBrandName(
                brand != null
                        ? brand.getName()
                        : null
        );

        response.setBaseUnit(
                request.getBaseUnit()
                        .trim()
                        .toUpperCase(Locale.ROOT)
        );

        response.setPurchasePrice(
                request.getPurchasePrice()
        );

        response.setSellingPrice(
                request.getSellingPrice()
        );

        response.setRejectionReason(null);

        return response;
    }

    public ProductPageResponse toPageResponse(
            Page<Products> productPage
    ) {
        return ProductPageResponse.builder()
                .content(
                        productPage.getContent()
                                .stream()
                                .map(this::toResponse)
                                .toList()
                )
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(
                        productPage.getTotalElements()
                )
                .totalPages(
                        productPage.getTotalPages()
                )
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }

    public ProductChangeRequestResponse
    toChangeRequestResponse(
            ProductChangeRequests changeRequest
    ) {
        Users approvedBy =
                changeRequest.getApprovedBy();

        return ProductChangeRequestResponse.builder()
                .id(changeRequest.getId())
                .requestCode(
                        changeRequest
                                .getProductChangeRequestsCode()
                )
                .product(
                        toResponse(
                                changeRequest.getProduct()
                        )
                )
                .oldValue(
                        changeRequest.getOldValue()
                )
                .newValue(
                        changeRequest.getNewValue()
                )
                .requestReason(
                        changeRequest.getRequestReason()
                )
                .rejectionReason(
                        changeRequest.getRejectionReason()
                )
                .status(changeRequest.getStatus())
                .createdById(
                        changeRequest.getCreatedBy().getId()
                )
                .createdByName(
                        changeRequest.getCreatedBy().getName()
                )
                .approvedById(
                        approvedBy != null
                                ? approvedBy.getId()
                                : null
                )
                .approvedByName(
                        approvedBy != null
                                ? approvedBy.getName()
                                : null
                )
                .createdAt(
                        changeRequest.getCreatedAt()
                )
                .approvedAt(
                        changeRequest.getApprovedAt()
                )
                .build();
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}