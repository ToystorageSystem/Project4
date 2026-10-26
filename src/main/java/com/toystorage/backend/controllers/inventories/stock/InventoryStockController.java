package com.toystorage.backend.controllers.inventories.stock;

import com.toystorage.backend.dto.response.inventories.stock.InventoryStockResponse;
import com.toystorage.backend.services.inventories.stock.InventoryStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/inventory")
@RequiredArgsConstructor
public class InventoryStockController {
    private final InventoryStockService stockService;

    @GetMapping("/balances")
    public ResponseEntity<List<InventoryStockResponse>> listBalances() {
        return ResponseEntity.ok(stockService.listCurrentWarehouseStock());
    }
}
