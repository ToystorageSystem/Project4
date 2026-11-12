package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderOptionResponse {

    private Long id;

    private String code;

    private String name;
}