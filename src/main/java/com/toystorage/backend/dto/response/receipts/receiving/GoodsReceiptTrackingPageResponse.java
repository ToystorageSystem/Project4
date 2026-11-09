package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptTrackingPageResponse {
    private List<GoodsReceiptSummaryResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
