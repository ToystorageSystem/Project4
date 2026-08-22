package com.toystorage.backend.dto.response.inventories.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportPageResponse {

    private List<InventoryReportItemResponse> items;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

    private Boolean first;

    private Boolean last;
}