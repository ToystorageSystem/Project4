package com.toystorage.backend.dto.response.warehouses;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {

    private Long id;

    private String warehousesCode;

    private String name;

    private WarehouseType type;

    private String address;

    private String phone;

    private WarehouseStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}