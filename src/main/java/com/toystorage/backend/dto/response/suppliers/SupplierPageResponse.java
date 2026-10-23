package com.toystorage.backend.dto.response.suppliers;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SupplierPageResponse {

    private List<SupplierResponse> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean first;

    private boolean last;
}