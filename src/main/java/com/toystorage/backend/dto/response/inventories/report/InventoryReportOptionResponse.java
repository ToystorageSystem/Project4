package com.toystorage.backend.dto.response.inventories.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportOptionResponse {

    private Long id;

    private String code;

    private String name;

    /**
     * Dùng chủ yếu cho warehouse/store.
     *
     * Ví dụ:
     * MAIN_WAREHOUSE
     * STORE
     *
     * Với category hoặc brand thì có thể null.
     */
    private String type;
}