package com.toystorage.backend.mapper.products;

import com.toystorage.backend.dto.response.products.CategoryResponse;
import com.toystorage.backend.entity.products.Categories;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(
            Categories category
    ) {
        return CategoryResponse.builder()
                .id(category.getId())
                .categoryCode(
                        category.getCategoriesCode()
                )
                .name(category.getName())
                .description(
                        category.getDescription()
                )
                .status(
                        category.getStatus().name()
                )
                .build();
    }
}