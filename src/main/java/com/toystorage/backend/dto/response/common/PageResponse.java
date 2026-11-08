package com.toystorage.backend.dto.response.common;

import java.util.List;

import org.springframework.data.domain.Page;

/** Stable JSON for paged lists (Spring's Page JSON has many extra fields). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}