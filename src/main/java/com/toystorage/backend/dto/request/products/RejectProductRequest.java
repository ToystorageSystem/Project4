package com.toystorage.backend.dto.request.products;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectProductRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(
            max = 500,
            message = "Lý do từ chối không được vượt quá 500 ký tự"
    )
    private String rejectionReason;
}