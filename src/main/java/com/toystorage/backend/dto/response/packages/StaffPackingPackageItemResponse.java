package com.toystorage.backend.dto.response.packages;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

public class StaffPackingPackageItemResponse {



        private Long productId;

        private String productCode;

        private String productName;

        private String barcode;

        private Integer quantity;
}
