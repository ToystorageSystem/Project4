package com.toystorage.backend.services.products;

import com.toystorage.backend.dto.request.products.CreateBrandRequest;
import com.toystorage.backend.dto.request.products.UpdateBrandRequest;
import com.toystorage.backend.dto.response.products.BrandPageResponse;
import com.toystorage.backend.dto.response.products.BrandResponse;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.products.BrandMapper;
import com.toystorage.backend.repository.products.BrandRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// Quan ly thuong hieu (Brand) - tuong tu Category Management
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public BrandPageResponse getBrands(String keyword, String status, int page, int size) {
        if (page < 0) {
            throw new BadRequest("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequest("Size must be between 1 and 100");
        }

        Page<Brands> result = brandRepository.search(
                normalizeNullable(keyword),
                parseStatus(status),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );

        return BrandPageResponse.builder()
                .content(result.getContent().stream().map(brandMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrand(Long brandId) {
        return brandMapper.toResponse(findBrand(brandId));
    }

    // Chi lay brand ACTIVE cho dropdown cua form Product
    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findByStatusOrderByNameAsc(CommonStatus.ACTIVE)
                .stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        String code = normalizeRequired(request.getBrandCode(), "Brand code is required")
                .toUpperCase(Locale.ROOT);
        String name = normalizeRequired(request.getName(), "Brand name is required");

        if (brandRepository.existsByBrandsCodeIgnoreCase(code)) {
            throw new BadRequest("Brand code already exists");
        }
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequest("Brand name already exists");
        }

        Brands brand = Brands.builder()
                .brandsCode(code)
                .name(name)
                .country(normalizeNullable(request.getCountry()))
                .description(normalizeNullable(request.getDescription()))
                .status(CommonStatus.ACTIVE)
                .build();

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateBrand(Long brandId, UpdateBrandRequest request) {
        Brands brand = findBrand(brandId);
        String code = normalizeRequired(request.getBrandCode(), "Brand code is required")
                .toUpperCase(Locale.ROOT);
        String name = normalizeRequired(request.getName(), "Brand name is required");

        if (brandRepository.existsByBrandsCodeIgnoreCaseAndIdNot(code, brandId)) {
            throw new BadRequest("Brand code already exists");
        }
        if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, brandId)) {
            throw new BadRequest("Brand name already exists");
        }

        brand.setBrandsCode(code);
        brand.setName(name);
        brand.setCountry(normalizeNullable(request.getCountry()));
        brand.setDescription(normalizeNullable(request.getDescription()));

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse deactivateBrand(Long brandId) {
        Brands brand = findBrand(brandId);
        if (brand.getStatus() == CommonStatus.INACTIVE) {
            throw new BadRequest("Brand is already inactive");
        }

        // Khong cho an brand khi con san pham dang cho duyet
        boolean hasPendingProducts = productRepository.existsByBrand_IdAndStatusIn(
                brandId,
                List.of(ProductStatus.PENDING, ProductStatus.PENDING_CREATE, ProductStatus.PENDING_UPDATE)
        );
        if (hasPendingProducts) {
            throw new BadRequest("Brand cannot be deactivated because it is related to pending products");
        }

        brand.setStatus(CommonStatus.INACTIVE);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse activateBrand(Long brandId) {
        Brands brand = findBrand(brandId);
        if (brand.getStatus() == CommonStatus.ACTIVE) {
            throw new BadRequest("Brand is already active");
        }
        brand.setStatus(CommonStatus.ACTIVE);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    private Brands findBrand(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new BadRequest("Brand id must be greater than 0");
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new NotFound("Brand not found with id: " + brandId));
    }

    private CommonStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return CommonStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequest("Status must be ACTIVE or INACTIVE");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequest(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
