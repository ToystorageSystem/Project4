package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PutawayTaskItemRepository
        extends JpaRepository<PutawayTaskItems, Long> {
    List<PutawayTaskItems>
    findByPutawayTaskId(Long putawayTaskId);

    long countByPutawayTaskId(Long putawayTaskId);
}