package com.toystorage.backend.dto.response.shipments;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InTransitShipmentPageResponse {

    @Builder.Default
    private List<InTransitShipmentListItemResponse> items =
            new ArrayList<>();

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean first;

    private boolean last;
}