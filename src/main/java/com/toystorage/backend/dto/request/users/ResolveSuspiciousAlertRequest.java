package com.toystorage.backend.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveSuspiciousAlertRequest {

    @NotBlank(
            message = "Ghi chú xử lý không được để trống"
    )
    @Size(
            max = 2000,
            message = "Ghi chú xử lý không được vượt quá 2000 ký tự"
    )
    private String resolutionNote;
}