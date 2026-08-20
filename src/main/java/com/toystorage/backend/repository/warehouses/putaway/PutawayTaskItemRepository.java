package com.toystorage.backend.repository.warehouses.putaway;

import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PutawayTaskItemRepository
        extends JpaRepository<PutawayTaskItems, Long> {
    List<PutawayTaskItems>
    findByPutawayTaskId(Long putawayTaskId);

    long countByPutawayTaskId(Long putawayTaskId);

    Optional<PutawayTaskItems>
    findByIdAndPutawayTaskId(
            Long itemId,
            Long putawayTaskId
    );

}