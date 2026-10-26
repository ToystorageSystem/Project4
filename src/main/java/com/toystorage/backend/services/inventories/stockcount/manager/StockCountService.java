package com.toystorage.backend.services.inventories.stockcount.manager;

import com.toystorage.backend.dto.request.inventories.stockcount.StockCountRequest;
import com.toystorage.backend.dto.response.inventories.stockcount.StockCountResponse;
import com.toystorage.backend.dto.response.inventories.stockcount.StockCountItemResponse;
import com.toystorage.backend.mapper.inventories.stockcount.manager.StockCountMapper;
import com.toystorage.backend.repository.inventories.stockcount.StockCountItemRepository;
import com.toystorage.backend.repository.inventories.stockcount.StockCountRepository;
import com.toystorage.backend.stockcount.StockCountWorkflowService;
import com.toystorage.backend.stockcount.StockCountDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

// Compatibility layer: older /api/stock-counts endpoints share the safe workflow.
@Service @RequiredArgsConstructor
public class StockCountService {
    private final StockCountRepository plans;
    private final StockCountItemRepository items;
    private final StockCountMapper mapper;
    private final StockCountValidationService access;
    private final StockCountWorkflowService workflow;

    @Transactional(readOnly = true)
    public StockCountResponse getStockCount(Long id) {
        workflow.detail(id); // also verifies manager role or staff assignment
        var plan = access.getStockCount(id);
        access.validateWarehouse(access.getCurrentUser(), plan);
        return mapper.toResponse(plan, items.findByStockCountId(id));
    }
    @Transactional
    public StockCountResponse start(Long id) {
        workflow.start(id);
        return getStockCount(id);
    }
    @Transactional
    public StockCountItemResponse countProduct(Long id, Long itemId, StockCountRequest request) {
        workflow.count(id, itemId, new StockCountDTO.CountRequest(request.getQuantity(), null, null));
        return mapper.toItemResponse(items.findById(itemId).orElseThrow());
    }
    @Transactional
    public StockCountResponse finishCounting(Long id) {
        workflow.submit(id);
        return getStockCount(id);
    }
    @Transactional
//    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    public StockCountResponse confirm(Long id) {
        workflow.confirm(id);
        return getStockCount(id);
    }
}
