package com.toystorage.backend.repository.warehouses.manager;

import com.toystorage.backend.entity.warehouses.StaffPerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffPerformanceReviewRepository extends JpaRepository<StaffPerformanceReview, Long> {

    @Query("""
        SELECT r FROM StaffPerformanceReview r JOIN FETCH r.staff JOIN FETCH r.reviewer
        WHERE r.warehouse.id = :warehouseId ORDER BY r.createdAt DESC
        """)
    List<StaffPerformanceReview> findForWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("""
        SELECT r FROM StaffPerformanceReview r JOIN FETCH r.staff JOIN FETCH r.reviewer
        WHERE r.staff.id = :staffId ORDER BY r.createdAt DESC
        """)
    List<StaffPerformanceReview> findForStaff(@Param("staffId") Long staffId);

}
