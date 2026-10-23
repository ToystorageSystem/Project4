package com.toystorage.backend.dto.response.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InTransitStatusHistoryResponse {

    private String status;

    private LocalDateTime changedAt;

    private Long changedById;

    private String changedByCode;

    private String changedByName;

    private String note;
}