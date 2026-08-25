package com.toystorage.backend.dto.request.deliveries.handover;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteDeliveryHandoverRequest {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    private String note;
}