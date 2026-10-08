package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * bao gồm PENDING / APPROVED / REJECTED.
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

    /**
     * Task #4:
     * Lấy request REJECTED hiện tại của từng sản phẩm.
     *
     * Nếu một request REJECTED cũ đã có request mới hơn,
     * request cũ chỉ còn là lịch sử và không xuất hiện
     * trong danh sách cần xử lý nữa.
     */
    @EntityGraph(attributePaths = {
            "product",
            "product.category",
            "product.brand",
            "createdBy",
            "approvedBy"
    })
    @Query(
            value = """
                    select r
                    from ProductChangeRequests r
                    where r.status = :status
                      and not exists (
                          select newer.id
                          from ProductChangeRequests newer
                          where newer.product.id = r.product.id
                            and (
                                newer.createdAt > r.createdAt
                                or (
                                    newer.createdAt = r.createdAt
                                    and newer.id > r.id
                                )
                            )
                      )
                    order by r.approvedAt desc, r.createdAt desc
                    """,
            countQuery = """
                    select count(r)
                    from ProductChangeRequests r
                    where r.status = :status
                      and not exists (
                          select newer.id
                          from ProductChangeRequests newer
                          where newer.product.id = r.product.id
                            and (
                                newer.createdAt > r.createdAt
                                or (
                                    newer.createdAt = r.createdAt
                                    and newer.id > r.id
                                )
                            )
                      )
                    """
    )
    Page<ProductChangeRequests> findLatestRequestsByStatus(
            @Param("status") ProductChangeRequestStatus status,
            Pageable pageable
    );

    /**
     * Kiểm tra một product có request đang PENDING hay không.
     *
     * Dùng để tránh trường hợp một sản phẩm
     * có nhiều request chờ duyệt cùng lúc.
     */
    boolean existsByProduct_IdAndStatus(
            Long productId,
            ProductChangeRequestStatus status
    );

    /**
     * Lấy request mới nhất của một product.
     *
     * Dùng để kiểm tra request REJECTED mà Staff đang xử lý
     * có còn là request mới nhất hay không.
     */
    Optional<ProductChangeRequests> findTopByProduct_IdOrderByCreatedAtDescIdDesc(
            Long productId
    );

    /**
     * Kiểm tra mã ProductChangeRequest đã tồn tại hay chưa.
     *
     * Dùng khi sinh mã PCR mới lúc resubmit.
     */
    boolean existsByProductChangeRequestsCode(
            String productChangeRequestsCode
    );

}