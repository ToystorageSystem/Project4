package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductChangeRequestRepository
        extends JpaRepository<ProductChangeRequests, Long> {

    /**
     * Lấy danh sách yêu cầu thay đổi sản phẩm theo trạng thái.
     *
     * Task Product Approval chủ yếu sử dụng:
     * PENDING -> các yêu cầu Business Manager cần xử lý.
     */
    @EntityGraph(attributePaths = {
            "product",
            "product.category",
            "product.brand",
            "createdBy",
            "approvedBy"
    })
    Page<ProductChangeRequests> findByStatusOrderByCreatedAtDesc(
            ProductChangeRequestStatus status,
            Pageable pageable
    );

    /**
     * Lấy chi tiết một yêu cầu cùng các quan hệ cần thiết
     * để tránh lỗi LazyInitialization khi map response.
     */
    @Override
    @EntityGraph(attributePaths = {
            "product",
            "product.category",
            "product.brand",
            "createdBy",
            "approvedBy"
    })
    Optional<ProductChangeRequests> findById(Long id);
     /**
     * Business Staff xem lại các request do chính mình tạo,
     * bao gồm cả PENDING / APPROVED / REJECTED.
     */
    @EntityGraph(attributePaths = {
            "product",
            "product.category",
            "product.brand",
            "createdBy",
            "approvedBy"
    })
    Page<ProductChangeRequests> findByCreatedBy_IdOrderByCreatedAtDesc(
            Long createdById,
            Pageable pageable
    );

}