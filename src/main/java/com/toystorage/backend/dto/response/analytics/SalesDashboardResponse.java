package com.toystorage.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesDashboardResponse {

    private Long storeId;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private Long totalOrders;

    private Long draftOrders;

    private Long pendingPaymentOrders;

    private Long completedOrders;

    private Long cancelledOrders;

    private Long refundedOrders;

    private BigDecimal completedRevenue;

    private Long completedSoldQuantity;

    private BigDecimal averageCompletedOrderValue;
}