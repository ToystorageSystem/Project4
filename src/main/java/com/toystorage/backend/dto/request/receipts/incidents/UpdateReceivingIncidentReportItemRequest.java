package com.toystorage.backend.dto.request.receipts.incidents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReceivingIncidentReportItemRequest {

    @NotBlank
    @Size(max = 4000)
    private String reason;
}
