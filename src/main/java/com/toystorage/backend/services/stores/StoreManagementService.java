package com.toystorage.backend.services.stores;

import com.toystorage.backend.dto.stores.StoreDetailsRequest;
import com.toystorage.backend.dto.stores.StoreDetailsResponse;
import com.toystorage.backend.entity.stores.Stores;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.repository.stores.StoreRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreManagementService {
    private final StoreRepository storeRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<StoreDetailsResponse> list() {
        return storeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StoreDetailsResponse get(Long warehouseId) {
        return toResponse(storeRepository.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found")));
    }

    // Call after the associated Warehouses row exists and is type STORE.
    @Transactional
    public StoreDetailsResponse upsert(Long warehouseId, StoreDetailsRequest request) {
        Warehouses warehouse = entityManager.find(Warehouses.class, warehouseId);
        if (warehouse == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
        if (warehouse.getType() != WarehouseType.STORE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location must have type STORE");
        Stores store = storeRepository.findById(warehouseId).orElseGet(() -> {
            Stores s = new Stores();
            s.setWarehouse(warehouse);
            return s;
        });
        store.setStoreManagerName(request.storeManagerName());
        store.setOpeningHours(request.openingHours());
        store.setStorePhone(request.storePhone());
        return toResponse(storeRepository.save(store));
    }

    private StoreDetailsResponse toResponse(Stores store) {
        Warehouses w = store.getWarehouse();
        return new StoreDetailsResponse(w.getId(), w.getWarehousesCode(), w.getName(),
                store.getStoreManagerName(), store.getOpeningHours(), store.getStorePhone());
    }
}
