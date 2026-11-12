package com.toystorage.backend.mapper.products;

import com.toystorage.backend.dto.response.products.BrandResponse;
import com.toystorage.backend.entity.products.Brands;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public BrandResponse toResponse(Brands brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .brandCode(brand.getBrandsCode())
                .name(brand.getName())
                .country(brand.getCountry())
                .description(brand.getDescription())
                .status(brand.getStatus() == null ? null : brand.getStatus().name())
                .build();
    }
}
