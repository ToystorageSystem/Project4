package com.toystorage.backend.dto.response.suppliers;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SupplierResponse {

    private Long id;

    private String supplierCode;

    private String name;

    private String taxCode;

    private String contactPerson;

    private String contactPosition;

    private String phone;

    private String email;

    private String address;

    private String note;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}