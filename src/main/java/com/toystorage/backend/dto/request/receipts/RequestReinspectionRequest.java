package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestReinspectionRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}