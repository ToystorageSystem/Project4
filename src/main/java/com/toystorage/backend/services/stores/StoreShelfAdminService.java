package com.toystorage.backend.services.stores;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.dto.request.stores.ShelfRequest;
import com.toystorage.backend.dto.request.stores.ShelfStatusRequest;
import com.toystorage.backend.dto.response.stores.ShelfAdminItem;
import com.toystorage.backend.entity.stores.StoreShelves;
import com.toystorage.backend.enums.stores.ShelfStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.receipts.store.StoreShelfRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;
import com.toystorage.backend.repository.stores.ShelfUsage;

import lombok.RequiredArgsConstructor;

/**
 * Shelf management – Store Manager only (2026-11-13).
 * Create, rename, deactivate / reactivate. No delete (keeps the stock history).
 * Staff keep using GET /api/store/shelves (active shelves only).
 */
@Service
@RequiredArgsConstructor
public class StoreShelfAdminService {

    private final StoreAccessService storeAccessService;
    private final StoreShelfRepository shelfRepository;
    private final StoreStockRepository stockRepository;

    /** GET /api/store/shelves/manage */
    @Transactional(readOnly = true)
    public List<ShelfAdminItem> listAll(Long userId) {
        Long storeId = storeAccessService.requireStoreManager(userId).getWarehouse().getId();
        Map<Long, ShelfUsage> usage = stockRepository.sumByShelf(storeId).stream()
                .collect(Collectors.toMap(ShelfUsage::shelfId, Function.identity()));
        return shelfRepository.findByStoreIdOrderByNameAsc(storeId).stream()
                .map(shelf -> toItem(shelf, usage.get(shelf.getId())))
                .toList();
    }

    /** POST /api/store/shelves */
    @Transactional
    public ShelfAdminItem create(Long userId, ShelfRequest request) {
        Long storeId = storeAccessService.requireStoreManager(userId).getWarehouse().getId();
        String name = request.name().trim();
        if (shelfRepository.existsByStoreIdAndNameIgnoreCase(storeId, name)) {
            throw new BadRequest("Shelf name already exists");
        }
        StoreShelves shelf = shelfRepository.save(
                StoreShelves.create(storeId, newCode(storeId), name, normalize(request.description())));
        return toItem(shelf, null);
    }

    /** PUT /api/store/shelves/{id} */
    @Transactional
    public ShelfAdminItem update(Long userId, Long shelfId, ShelfRequest request) {
        Long storeId = storeAccessService.requireStoreManager(userId).getWarehouse().getId();
        StoreShelves shelf = requireShelf(storeId, shelfId);
        String name = request.name().trim();
        if (shelfRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(storeId, name, shelfId)) {
            throw new BadRequest("Shelf name already exists");
        }
        shelf.rename(name, normalize(request.description()));
        return toItem(shelf, usageOf(storeId, shelfId));
    }

    /** PATCH /api/store/shelves/{id}/status */
    @Transactional
    public ShelfAdminItem changeStatus(Long userId, Long shelfId, ShelfStatusRequest request) {
        Long storeId = storeAccessService.requireStoreManager(userId).getWarehouse().getId();
        StoreShelves shelf = requireShelf(storeId, shelfId);
        if (request.status() == ShelfStatus.INACTIVE
                && !stockRepository.findByStoreIdAndShelf_IdOrderByProduct_NameAsc(storeId, shelfId).isEmpty()) {
            throw new BadRequest("Move products off this shelf first");
        }
        shelf.changeStatus(request.status());
        return toItem(shelf, usageOf(storeId, shelfId));
    }

    // ---------- helpers ----------

    private StoreShelves requireShelf(Long storeId, Long shelfId) {
        return shelfRepository.findByIdAndStoreId(shelfId, storeId)
                .orElseThrow(() -> new NotFound("Shelf not found"));
    }

    private ShelfUsage usageOf(Long storeId, Long shelfId) {
        return stockRepository.sumByShelf(storeId).stream()
                .filter(u -> shelfId.equals(u.shelfId()))
                .findFirst().orElse(null);
    }

    /** SHF-<store>-001, SHF-<store>-002 ... (skips codes already used) */
    private String newCode(Long storeId) {
        long next = shelfRepository.countByStoreId(storeId) + 1;
        String code;
        do {
            code = String.format("SHF-%d-%03d", storeId, next++);
        } while (shelfRepository.existsByCode(code));
        return code;
    }

    private static ShelfAdminItem toItem(StoreShelves shelf, ShelfUsage usage) {
        long products = usage == null || usage.productCount() == null ? 0 : usage.productCount();
        long quantity = usage == null || usage.shelfQuantity() == null ? 0 : usage.shelfQuantity();
        return new ShelfAdminItem(shelf.getId(), shelf.getCode(), shelf.getName(), shelf.getDescription(),
                shelf.getStatus(), products, quantity);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
