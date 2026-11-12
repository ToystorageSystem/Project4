package com.toystorage.backend.dto.request.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectProductApprovalRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    private String reason;

    public RejectProductApprovalRequest() {
    }

    public RejectProductApprovalRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}