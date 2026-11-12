package com.toystorage.backend.repository.warehouses.manager;

import com.toystorage.backend.entity.warehouses.StaffShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StaffShiftAssignmentRepository extends JpaRepository<StaffShiftAssignment, Long> {

    @Query("""
        SELECT s FROM StaffShiftAssignment s JOIN FETCH s.staff
        WHERE s.warehouse.id = :warehouseId AND s.workDate = :date
        """)
    List<StaffShiftAssignment> findForWarehouseDay(@Param("warehouseId") Long warehouseId, @Param("date") LocalDate date);

    @Query("SELECT s FROM StaffShiftAssignment s WHERE s.staff.id = :staffId AND s.workDate = :date")
    List<StaffShiftAssignment> findForStaffDay(@Param("staffId") Long staffId, @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM StaffShiftAssignment s WHERE s.staff.id = :staffId AND s.workDate = :date")
    void deleteForStaffDay(@Param("staffId") Long staffId, @Param("date") LocalDate date);
}
