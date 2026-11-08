package com.toystorage.backend.dto.request.businessmanager;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ApproveStoreReturnRequest {

    @NotEmpty(message = "Approval items must not be empty")
    @Valid
    private List<ItemApproval> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemApproval {

        @NotNull
        @Positive
        private Long storeReturnItemId;

        @NotNull
        @Min(0)
        private Integer approvedQuantity;
    }
}
