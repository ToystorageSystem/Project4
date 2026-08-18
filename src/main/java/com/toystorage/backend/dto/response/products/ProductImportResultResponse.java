package com.toystorage.backend.dto.response.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportResultResponse {

    private int totalRows;

    private int importedRows;

    @Builder.Default
    private List<ProductChangeRequestResponse> changeRequests =
            new ArrayList<>();
}