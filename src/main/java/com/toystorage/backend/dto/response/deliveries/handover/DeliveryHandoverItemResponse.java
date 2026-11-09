package com.toystorage.backend.dto.response.deliveries.handover;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryHandoverItemResponse {

    private Long packageId;

    private String packageCode;

    private String expectedSealNumber;

    private String actualSealNumber;

    private String condition;

    private String issueNote;

    private LocalDateTime scannedAt;
}