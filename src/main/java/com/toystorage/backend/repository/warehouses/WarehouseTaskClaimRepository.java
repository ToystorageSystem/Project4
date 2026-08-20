package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseTaskClaimRepository
        extends JpaRepository<WarehouseTaskClaim, Long> {

    Optional<WarehouseTaskClaim>
    findByTaskTypeAndReferenceId(
            WarehouseTaskType taskType,
            Long referenceId
    );


    boolean existsByTaskTypeAndReferenceId(
            WarehouseTaskType taskType,
            Long referenceId
    );


    void deleteByTaskTypeAndReferenceId(
            WarehouseTaskType taskType,
            Long referenceId
    );
}