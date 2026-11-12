package com.toystorage.backend.controllers.products;

import com.toystorage.backend.dto.request.products.CreateBrandRequest;
import com.toystorage.backend.dto.request.products.UpdateBrandRequest;
import com.toystorage.backend.dto.response.products.BrandPageResponse;
import com.toystorage.backend.dto.response.products.BrandResponse;
import com.toystorage.backend.services.products.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<BrandPageResponse> getBrands(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandService.getBrands(keyword, status, page, size));
    }

    @GetMapping("/options")
    public ResponseEntity<List<BrandResponse>> getActiveBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    @GetMapping("/{brandId}")
    public ResponseEntity<BrandResponse> getBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(brandService.getBrand(brandId));
    }

    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(request));
    }

    @PutMapping("/{brandId}")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        return ResponseEntity.ok(brandService.updateBrand(brandId, request));
    }

    @PatchMapping("/{brandId}/deactivate")
    public ResponseEntity<BrandResponse> deactivateBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(brandService.deactivateBrand(brandId));
    }

    @PatchMapping("/{brandId}/activate")
    public ResponseEntity<BrandResponse> activateBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(brandService.activateBrand(brandId));
    }
}
