package com.toystorage.backend.repository.warehouses.taskclaim;

import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseTaskClaimRepository
        extends JpaRepository<WarehouseTaskClaim, Long> {

    /*
     * =====================================================
     * ACTIVE CLAIM
     * =====================================================
     *
     * Lấy task hiện đang được Staff xử lý.
     *
     * releasedAt == null
     * => task vẫn đang active.
     */
    Optional<WarehouseTaskClaim>
    findFirstByTaskTypeAndReferenceIdAndReleasedAtIsNull(
            WarehouseTaskType taskType,
            Long referenceId
    );


    /*
     * =====================================================
     * CHECK ACTIVE CLAIM
     * =====================================================
     *
     * Dùng để kiểm tra task hiện tại đã có Staff claim chưa.
     */
    boolean existsByTaskTypeAndReferenceIdAndReleasedAtIsNull(
            WarehouseTaskType taskType,
            Long referenceId
    );


    /*
     * =====================================================
     * LATEST CLAIM / LATEST ATTEMPT
     * =====================================================
     *
     * Lấy lần claim gần nhất.
     *
     * Ví dụ:
     *
     * attempt 1 -> Staff A
     * attempt 2 -> Staff B
     *
     * Method này trả attempt 2.
     */
    Optional<WarehouseTaskClaim>
    findFirstByTaskTypeAndReferenceIdOrderByAttemptNoDesc(
            WarehouseTaskType taskType,
            Long referenceId
    );


    /*
     * =====================================================
     * CLAIM HISTORY
     * =====================================================
     *
     * Lấy toàn bộ lịch sử xử lý task.
     *
     * Có thể dùng cho:
     * - Receiving history
     * - KPI
     * - Audit
     * - Re-inspection history
     */
    List<WarehouseTaskClaim>
    findAllByTaskTypeAndReferenceIdOrderByAttemptNoAsc(
            WarehouseTaskType taskType,
            Long referenceId
    );


    /*
     * =====================================================
     * COUNT ATTEMPTS
     * =====================================================
     *
     * Có thể dùng để biết task đã được thực hiện bao nhiêu lần.
     */
    long countByTaskTypeAndReferenceId(
            WarehouseTaskType taskType,
            Long referenceId
    );
}