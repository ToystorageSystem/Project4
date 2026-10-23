package com.toystorage.backend.controllers.products;

import com.toystorage.backend.dto.request.products.CreateCategoryRequest;
import com.toystorage.backend.dto.request.products.UpdateCategoryRequest;
import com.toystorage.backend.dto.response.products.CategoryPageResponse;
import com.toystorage.backend.dto.response.products.CategoryResponse;
import com.toystorage.backend.services.products.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyRole('BS_STAFF', 'ADMIN')"
)
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<CategoryPageResponse>
    getCategories(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                categoryService.getCategories(
                        keyword,
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse>
    getCategory(
            @PathVariable
            Long categoryId
    ) {

        return ResponseEntity.ok(
                categoryService.getCategory(
                        categoryId
                )
        );
    }

    @GetMapping("/options")
    public ResponseEntity<List<CategoryResponse>>
    getActiveCategories() {

        return ResponseEntity.ok(
                categoryService
                        .getActiveCategories()
        );
    }

    @PostMapping
    public ResponseEntity<CategoryResponse>
    createCategory(
            @Valid
            @RequestBody
            CreateCategoryRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        categoryService
                                .createCategory(
                                        request
                                )
                );
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse>
    updateCategory(
            @PathVariable
            Long categoryId,

            @Valid
            @RequestBody
            UpdateCategoryRequest request
    ) {

        return ResponseEntity.ok(
                categoryService.updateCategory(
                        categoryId,
                        request
                )
        );
    }

    @PatchMapping(
            "/{categoryId}/deactivate"
    )
    public ResponseEntity<CategoryResponse>
    deactivateCategory(
            @PathVariable
            Long categoryId
    ) {

        return ResponseEntity.ok(
                categoryService
                        .deactivateCategory(
                                categoryId
                        )
        );
    }

    @PatchMapping(
            "/{categoryId}/activate"
    )
    public ResponseEntity<CategoryResponse>
    activateCategory(
            @PathVariable
            Long categoryId
    ) {

        return ResponseEntity.ok(
                categoryService
                        .activateCategory(
                                categoryId
                        )
        );
    }
}