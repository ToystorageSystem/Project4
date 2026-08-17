package com.toystorage.backend.dto.request.transfers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportPickingIssueRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String issueType;

    @NotBlank
    private String description;
}