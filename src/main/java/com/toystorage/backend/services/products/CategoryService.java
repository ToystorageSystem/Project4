package com.toystorage.backend.services.products;

import com.toystorage.backend.dto.request.products.CreateCategoryRequest;
import com.toystorage.backend.dto.request.products.UpdateCategoryRequest;
import com.toystorage.backend.dto.response.products.CategoryPageResponse;
import com.toystorage.backend.dto.response.products.CategoryResponse;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.products.CategoryMapper;
import com.toystorage.backend.repository.products.CategoryRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryAuditService categoryAuditService;

    @Transactional(readOnly = true)
    public CategoryPageResponse getCategories(
            String keyword,
            String status,
            int page,
            int size
    ) {

        validatePagination(
                page,
                size
        );

        String normalizedKeyword =
                normalizeNullable(keyword);

        CommonStatus parsedStatus =
                parseStatus(status);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.ASC,
                                "name"
                        )
                );

        Page<Categories> result =
                categoryRepository.search(
                        normalizedKeyword,
                        parsedStatus,
                        pageable
                );

        List<CategoryResponse> content =
                result.getContent()
                        .stream()
                        .map(
                                categoryMapper::toResponse
                        )
                        .toList();

        return CategoryPageResponse.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(
                        result.getTotalElements()
                )
                .totalPages(
                        result.getTotalPages()
                )
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(
            Long categoryId
    ) {

        return categoryMapper.toResponse(
                findCategory(categoryId)
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse>
    getActiveCategories() {

        return categoryRepository
                .findByStatusOrderByNameAsc(
                        CommonStatus.ACTIVE
                )
                .stream()
                .map(
                        categoryMapper::toResponse
                )
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(
            CreateCategoryRequest request
    ) {

        String code =
                normalizeRequired(
                        request.getCategoryCode(),
                        "Category code is required"
                );

        String name =
                normalizeRequired(
                        request.getName(),
                        "Category name is required"
                );

        validateCreateDuplicate(
                code,
                name
        );

        Categories category =
                Categories.builder()
                        .categoriesCode(code)
                        .name(name)
                        .description(
                                normalizeNullable(
                                        request.getDescription()
                                )
                        )
                        .status(
                                CommonStatus.ACTIVE
                        )
                        .build();

        Categories saved =
                categoryRepository.save(category);

        categoryAuditService
                .logCreate(saved);

        return categoryMapper
                .toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(
            Long categoryId,
            UpdateCategoryRequest request
    ) {

        Categories category =
                findCategory(categoryId);

        Map<String, Object> oldValue =
                categoryAuditService
                        .snapshot(category);

        String code =
                normalizeRequired(
                        request.getCategoryCode(),
                        "Category code is required"
                );

        String name =
                normalizeRequired(
                        request.getName(),
                        "Category name is required"
                );

        validateUpdateDuplicate(
                categoryId,
                code,
                name
        );

        category.setCategoriesCode(code);
        category.setName(name);
        category.setDescription(
                normalizeNullable(
                        request.getDescription()
                )
        );

        Categories saved =
                categoryRepository.save(category);

        categoryAuditService.logUpdate(
                saved.getId(),
                oldValue,
                saved
        );

        return categoryMapper
                .toResponse(saved);
    }

    @Transactional
    public CategoryResponse deactivateCategory(
            Long categoryId
    ) {

        Categories category =
                findCategory(categoryId);

        if (category.getStatus()
                == CommonStatus.INACTIVE) {

            throw new BadRequest(
                    "Category is already inactive"
            );
        }

        boolean hasPendingProducts =
                productRepository
                        .existsByCategory_IdAndStatus(
                                categoryId,
                                ProductStatus.PENDING
                        );

        if (hasPendingProducts) {

            throw new BadRequest(
                    "Category cannot be deactivated "
                    + "because it is being used by "
                    + "products that are still pending"
            );
        }

        Map<String, Object> oldValue =
                categoryAuditService
                        .snapshot(category);

        category.setStatus(
                CommonStatus.INACTIVE
        );

        Categories saved =
                categoryRepository.save(category);

        categoryAuditService.logUpdate(
                saved.getId(),
                oldValue,
                saved
        );

        return categoryMapper
                .toResponse(saved);
    }

    @Transactional
    public CategoryResponse activateCategory(
            Long categoryId
    ) {

        Categories category =
                findCategory(categoryId);

        if (category.getStatus()
                == CommonStatus.ACTIVE) {

            throw new BadRequest(
                    "Category is already active"
            );
        }

        Map<String, Object> oldValue =
                categoryAuditService
                        .snapshot(category);

        category.setStatus(
                CommonStatus.ACTIVE
        );

        Categories saved =
                categoryRepository.save(category);

        categoryAuditService.logUpdate(
                saved.getId(),
                oldValue,
                saved
        );

        return categoryMapper
                .toResponse(saved);
    }

    private Categories findCategory(
            Long categoryId
    ) {

        if (categoryId == null
                || categoryId <= 0) {

            throw new BadRequest(
                    "Category id must be greater than 0"
            );
        }

        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new NotFound(
                                "Category not found with id: "
                                + categoryId
                        )
                );
    }

    private void validateCreateDuplicate(
            String code,
            String name
    ) {

        if (categoryRepository
                .existsByCategoriesCodeIgnoreCase(
                        code
                )) {

            throw new BadRequest(
                    "Category code already exists"
            );
        }

        if (categoryRepository
                .existsByNameIgnoreCase(
                        name
                )) {

            throw new BadRequest(
                    "Category name already exists"
            );
        }
    }

    private void validateUpdateDuplicate(
            Long categoryId,
            String code,
            String name
    ) {

        if (categoryRepository
                .existsByCategoriesCodeIgnoreCaseAndIdNot(
                        code,
                        categoryId
                )) {

            throw new BadRequest(
                    "Category code already exists"
            );
        }

        if (categoryRepository
                .existsByNameIgnoreCaseAndIdNot(
                        name,
                        categoryId
                )) {

            throw new BadRequest(
                    "Category name already exists"
            );
        }
    }

    private CommonStatus parseStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return null;
        }

        try {

            return CommonStatus.valueOf(
                    status.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException ex) {

            throw new BadRequest(
                    "Status must be ACTIVE or INACTIVE"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {

        if (page < 0) {

            throw new BadRequest(
                    "Page must be greater than "
                    + "or equal to 0"
            );
        }

        if (size < 1
                || size > 100) {

            throw new BadRequest(
                    "Size must be between 1 and 100"
            );
        }
    }

    private String normalizeRequired(
            String value,
            String message
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new BadRequest(message);
        }

        return value.trim();
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}