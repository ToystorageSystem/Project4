package com.toystorage.backend.dto.response.shipments;

import lombok.*;

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