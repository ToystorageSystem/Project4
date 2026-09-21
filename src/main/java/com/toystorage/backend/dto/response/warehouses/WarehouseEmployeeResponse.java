package com.toystorage.backend.dto.response.warehouses;

import com.toystorage.backend.enums.users.UserStatus;

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
public class WarehouseEmployeeResponse {

    private Long userId;

    private String userCode;

    private String name;

    private String email;

    private String phone;

    private UserStatus status;

    private Long warehouseId;

    private List<String> roles;

    private boolean manager;
}