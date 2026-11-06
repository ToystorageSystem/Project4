package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewPurchaseOrderCancellation {
    @Size(max = 1000, message = "Decision note cannot exceed 1000 characters")
    private String note;
}
