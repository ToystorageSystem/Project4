package com.toystorage.backend.dto.response.products;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BrandPageResponse {

    private List<BrandResponse> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean first;

    private boolean last;
}
