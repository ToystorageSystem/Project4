package com.toystorage.backend.repository.products;

import com.toystorage.backend.entity.products.ProductChangeRequests;
import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductChangeRequestRepository
        extends JpaRepository<ProductChangeRequests, Long> {

    Page<ProductChangeRequests> findByStatusOrderByCreatedAtDesc(
            ProductChangeRequestStatus status,
            Pageable pageable
    );

    Optional<ProductChangeRequests> findByIdAndStatus(
            Long id,
            ProductChangeRequestStatus status
    );

    boolean existsByProduct_IdAndStatus(
            Long productId,
            ProductChangeRequestStatus status
    );

    boolean existsByProductChangeRequestsCode(
            String productChangeRequestsCode
    );
}
