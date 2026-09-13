package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestReinspectionRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    /*
     * false:
     * Staff vừa kiểm lần trước không được nhận lại.
     *
     * true:
     * cho phép Staff cũ làm lại nếu kho thiếu người.
     */
    private Boolean allowSameStaff = false;

    public boolean isAllowSameStaff() {
        return Boolean.TRUE.equals(
                allowSameStaff
        );
    }
}