package com.toystorage.backend.dto.request.receipts.incidents;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReceivingIncidentReportRequest {

    @Size(max = 4000)
    private String penaltyAction;

    @Size(max = 4000)
    private String managerNote;
}
