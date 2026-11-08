package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.StockTransferOperationalAnalyticsResponse;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.analytics.StockTransferOperationalAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransferOperationalAnalyticsService {

    private final StockTransferOperationalAnalyticsRepository
            stockTransferOperationalAnalyticsRepository;


    // =========================================================
    // STOCK TRANSFER OPERATIONAL ANALYTICS
    // =========================================================

    public List<StockTransferOperationalAnalyticsResponse>
    getOperationalAnalytics(

            Long fromWarehouseId,

            Long toWarehouseId,

            TransferStatus status,

            TransferType transferType,

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        List<StockTransferOperationalAnalyticsRepository
                .StockTransferOperationalAnalyticsRow> rows =
                stockTransferOperationalAnalyticsRepository
                        .findOperationalAnalytics(

                                fromWarehouseId,

                                toWarehouseId,

                                status != null
                                        ? status.name()
                                        : null,

                                transferType != null
                                        ? transferType.name()
                                        : null,

                                fromDate,

                                toDate
                        );


        if (
                rows == null
                        || rows.isEmpty()
        ) {

            return new ArrayList<>();
        }


        List<StockTransferOperationalAnalyticsResponse> result =
                new ArrayList<>();


        for (
                StockTransferOperationalAnalyticsRepository
                        .StockTransferOperationalAnalyticsRow row
                : rows
        ) {

            result.add(
                    buildResponse(
                            row
                    )
            );
        }


        return result;
    }


    // =========================================================
    // BUILD RESPONSE
    // =========================================================

    private StockTransferOperationalAnalyticsResponse buildResponse(

            StockTransferOperationalAnalyticsRepository
                    .StockTransferOperationalAnalyticsRow row
    ) {

        long approvedQuantity =
                safeLong(
                        row.getTotalApprovedQuantity()
                );


        long shippedQuantity =
                safeLong(
                        row.getTotalShippedQuantity()
                );


        long receivedQuantity =
                safeLong(
                        row.getTotalReceivedQuantity()
                );


        // =====================================================
        // PROGRESS
        // =====================================================

        BigDecimal shippingProgressPercent =
                calculateRate(
                        shippedQuantity,
                        approvedQuantity
                );


        BigDecimal receivingProgressPercent =
                calculateRate(
                        receivedQuantity,
                        shippedQuantity
                );


        // =====================================================
        // SCHEDULE
        // =====================================================

        Boolean shipmentOverdue =
                calculateOverdue(
                        row.getExpectedShipmentDate(),
                        row.getShippedAt(),
                        row.getStatus()
                );


        Long shipmentDelayDays =
                calculateDelayDays(
                        row.getExpectedShipmentDate(),
                        row.getShippedAt(),
                        row.getStatus()
                );


        Boolean receiptOverdue =
                calculateOverdue(
                        row.getExpectedReceiptDate(),
                        row.getReceivedAt(),
                        row.getStatus()
                );


        Long receiptDelayDays =
                calculateDelayDays(
                        row.getExpectedReceiptDate(),
                        row.getReceivedAt(),
                        row.getStatus()
                );


        // =====================================================
        // ANALYSIS
        // =====================================================

        String operationalAnalysis =
                determineOperationalAnalysis(
                        row,
                        shipmentOverdue,
                        receiptOverdue
                );


        // =====================================================
        // RECOMMENDATION
        // =====================================================

        String recommendation =
                buildRecommendation(
                        operationalAnalysis,
                        row,
                        shipmentDelayDays,
                        receiptDelayDays
                );


        // =====================================================
        // RESPONSE
        // =====================================================

        return StockTransferOperationalAnalyticsResponse
                .builder()

                // =================================================
                // TRANSFER
                // =================================================

                .transferId(
                        row.getTransferId()
                )

                .transferCode(
                        row.getTransferCode()
                )

                .status(
                        row.getStatus()
                )

                .transferType(
                        row.getTransferType()
                )

                .reasonCode(
                        row.getReasonCode()
                )


                // =================================================
                // SOURCE WAREHOUSE
                // =================================================

                .fromWarehouseId(
                        row.getFromWarehouseId()
                )

                .fromWarehouseCode(
                        row.getFromWarehouseCode()
                )

                .fromWarehouseName(
                        row.getFromWarehouseName()
                )

                .fromWarehouseType(
                        row.getFromWarehouseType()
                )


                // =================================================
                // DESTINATION WAREHOUSE
                // =================================================

                .toWarehouseId(
                        row.getToWarehouseId()
                )

                .toWarehouseCode(
                        row.getToWarehouseCode()
                )

                .toWarehouseName(
                        row.getToWarehouseName()
                )

                .toWarehouseType(
                        row.getToWarehouseType()
                )


                // =================================================
                // TIMELINE
                // =================================================

                .createdAt(
                        row.getCreatedAt()
                )

                .confirmedAt(
                        row.getConfirmedAt()
                )

                .packingCompletedAt(
                        row.getPackingCompletedAt()
                )

                .shippedAt(
                        row.getShippedAt()
                )

                .receivedAt(
                        row.getReceivedAt()
                )


                // =================================================
                // EXPECTED DATES
                // =================================================

                .expectedShipmentDate(
                        row.getExpectedShipmentDate()
                )

                .expectedReceiptDate(
                        row.getExpectedReceiptDate()
                )


                // =================================================
                // PRODUCTS
                // =================================================

                .totalProducts(
                        safeLong(
                                row.getTotalProducts()
                        )
                )


                // =================================================
                // QUANTITY FLOW
                // =================================================

                .totalRequestedQuantity(
                        safeLong(
                                row.getTotalRequestedQuantity()
                        )
                )

                .totalApprovedQuantity(
                        approvedQuantity
                )

                .totalPickedQuantity(
                        safeLong(
                                row.getTotalPickedQuantity()
                        )
                )

                .totalPackedQuantity(
                        safeLong(
                                row.getTotalPackedQuantity()
                        )
                )

                .totalShippedQuantity(
                        shippedQuantity
                )

                .totalReceivedQuantity(
                        receivedQuantity
                )


                // =================================================
                // DISCREPANCY
                // =================================================

                .totalShortageQuantity(
                        safeLong(
                                row.getTotalShortageQuantity()
                        )
                )

                .totalSurplusQuantity(
                        safeLong(
                                row.getTotalSurplusQuantity()
                        )
                )


                // =================================================
                // PROGRESS
                // =================================================

                .shippingProgressPercent(
                        shippingProgressPercent
                )

                .receivingProgressPercent(
                        receivingProgressPercent
                )


                // =================================================
                // SCHEDULE
                // =================================================

                .shipmentOverdue(
                        shipmentOverdue
                )

                .shipmentDelayDays(
                        shipmentDelayDays
                )

                .receiptOverdue(
                        receiptOverdue
                )

                .receiptDelayDays(
                        receiptDelayDays
                )


                // =================================================
                // ANALYTICS
                // =================================================

                .operationalAnalysis(
                        operationalAnalysis
                )

                .recommendation(
                        recommendation
                )

                .build();
    }


    // =========================================================
    // OPERATIONAL ANALYSIS
    //
    // Không sử dụng ngưỡng thời gian tự đặt.
    //
    // Việc xác định overdue dựa trực tiếp trên:
    //
    // expected_shipment_date
    // expected_receipt_date
    // =========================================================

    private String determineOperationalAnalysis(

            StockTransferOperationalAnalyticsRepository
                    .StockTransferOperationalAnalyticsRow row,

            Boolean shipmentOverdue,

            Boolean receiptOverdue
    ) {

        String status =
                row.getStatus();


        // =====================================================
        // TERMINAL EXCEPTION STATUS
        // =====================================================

        if (
                "REJECTED".equals(
                        status
                )
        ) {

            return "REJECTED";
        }


        if (
                "CANCELLED".equals(
                        status
                )
        ) {

            return "CANCELLED";
        }


        // =====================================================
        // SHORTAGE
        // =====================================================

        if (
                safeLong(
                        row.getTotalShortageQuantity()
                ) > 0L
        ) {

            return "SHORTAGE";
        }


        // =====================================================
        // SURPLUS
        // =====================================================

        if (
                safeLong(
                        row.getTotalSurplusQuantity()
                ) > 0L
        ) {

            return "SURPLUS";
        }


        // =====================================================
        // RECEIPT OVERDUE
        // =====================================================

        if (
                Boolean.TRUE.equals(
                        receiptOverdue
                )
        ) {

            return "OVERDUE_RECEIPT";
        }


        // =====================================================
        // SHIPMENT OVERDUE
        // =====================================================

        if (
                Boolean.TRUE.equals(
                        shipmentOverdue
                )
        ) {

            return "OVERDUE_SHIPMENT";
        }


        long shippedQuantity =
                safeLong(
                        row.getTotalShippedQuantity()
                );


        long receivedQuantity =
                safeLong(
                        row.getTotalReceivedQuantity()
                );


        long approvedQuantity =
                safeLong(
                        row.getTotalApprovedQuantity()
                );


        // =====================================================
        // PARTIAL RECEIVING
        // =====================================================

        if (
                shippedQuantity > 0L
                        && receivedQuantity > 0L
                        && receivedQuantity < shippedQuantity
        ) {

            return "PARTIAL_RECEIVING";
        }


        // =====================================================
        // WAITING SHIPMENT
        // =====================================================

        if (
                approvedQuantity > 0L
                        && shippedQuantity == 0L
        ) {

            return "WAITING_SHIPMENT";
        }


        // =====================================================
        // COMPLETED NORMAL
        // =====================================================

        if (
                "COMPLETED".equals(
                        status
                )
                        && shippedQuantity
                        == receivedQuantity
        ) {

            return "COMPLETED_NORMAL";
        }


        // =====================================================
        // IN PROGRESS
        // =====================================================

        return "IN_PROGRESS";
    }


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    private String buildRecommendation(

            String operationalAnalysis,

            StockTransferOperationalAnalyticsRepository
                    .StockTransferOperationalAnalyticsRow row,

            Long shipmentDelayDays,

            Long receiptDelayDays
    ) {

        return switch (
                operationalAnalysis
        ) {

            case "SHORTAGE" ->

                    "Phiếu điều chuyển ghi nhận thiếu "
                            + safeLong(
                                    row.getTotalShortageQuantity()
                            )
                            + " đơn vị so với hàng đã gửi. "
                            + "Cần kiểm tra biên bản nhận hàng, kiện hàng "
                            + "và nguyên nhân chênh lệch.";


            case "SURPLUS" ->

                    "Phiếu điều chuyển ghi nhận dư "
                            + safeLong(
                                    row.getTotalSurplusQuantity()
                            )
                            + " đơn vị khi nhận hàng. "
                            + "Cần đối chiếu số lượng đóng gói, vận chuyển "
                            + "và số lượng thực nhận.";


            case "OVERDUE_RECEIPT" ->

                    "Phiếu điều chuyển đã trễ ngày nhận dự kiến"
                            + formatDelayDays(
                                    receiptDelayDays
                            )
                            + ". Cần kiểm tra trạng thái vận chuyển "
                            + "và phối hợp với kho nhận.";


            case "OVERDUE_SHIPMENT" ->

                    "Phiếu điều chuyển đã trễ ngày xuất hàng dự kiến"
                            + formatDelayDays(
                                    shipmentDelayDays
                            )
                            + ". Cần kiểm tra tiến độ picking, packing "
                            + "và bàn giao vận chuyển.";


            case "PARTIAL_RECEIVING" ->

                    "Kho nhận mới tiếp nhận "
                            + safeLong(
                                    row.getTotalReceivedQuantity()
                            )
                            + "/"
                            + safeLong(
                                    row.getTotalShippedQuantity()
                            )
                            + " đơn vị đã gửi. "
                            + "Cần tiếp tục theo dõi quá trình nhận hàng.";


            case "WAITING_SHIPMENT" ->

                    "Hàng đã được phê duyệt nhưng chưa ghi nhận xuất kho. "
                            + "Cần theo dõi bước picking, packing "
                            + "và bàn giao vận chuyển.";


            case "REJECTED" ->

                    "Phiếu điều chuyển đã bị từ chối. "
                            + "Cần kiểm tra lý do từ chối trước khi tạo "
                            + "hoặc xử lý yêu cầu điều chuyển tiếp theo.";


            case "CANCELLED" ->

                    "Phiếu điều chuyển đã bị hủy. "
                            + "Cần kiểm tra nguyên nhân hủy nếu cần "
                            + "thực hiện điều chuyển thay thế.";


            case "COMPLETED_NORMAL" ->

                    "Phiếu điều chuyển đã hoàn tất và chưa phát hiện "
                            + "chênh lệch số lượng trong dữ liệu hiện tại.";


            default ->

                    "Phiếu điều chuyển đang trong quá trình xử lý. "
                            + "Tiếp tục theo dõi tiến độ xác nhận, "
                            + "đóng gói, xuất hàng và nhận hàng.";
        };
    }


    // =========================================================
    // OVERDUE
    //
    // expectedDate == null
    //      → null
    //
    // Có actualDate:
    //      actualDate > expectedDate
    //      → true
    //
    // Chưa có actualDate:
    //
    //      CANCELLED / REJECTED
    //      → false
    //
    //      today > expectedDate
    //      → true
    // =========================================================

    private Boolean calculateOverdue(

            LocalDate expectedDate,

            LocalDateTime actualDateTime,

            String status
    ) {

        if (
                expectedDate == null
        ) {

            return null;
        }


        if (
                actualDateTime != null
        ) {

            return actualDateTime
                    .toLocalDate()
                    .isAfter(
                            expectedDate
                    );
        }


        if (
                isTerminalWithoutCompletion(
                        status
                )
        ) {

            return false;
        }


        return LocalDate
                .now()
                .isAfter(
                        expectedDate
                );
    }


    // =========================================================
    // DELAY DAYS
    // =========================================================

    private Long calculateDelayDays(

            LocalDate expectedDate,

            LocalDateTime actualDateTime,

            String status
    ) {

        if (
                expectedDate == null
        ) {

            return null;
        }


        LocalDate compareDate;


        if (
                actualDateTime != null
        ) {

            compareDate =
                    actualDateTime
                            .toLocalDate();

        } else {

            if (
                    isTerminalWithoutCompletion(
                            status
                    )
            ) {

                return 0L;
            }


            compareDate =
                    LocalDate.now();
        }


        if (
                !compareDate.isAfter(
                        expectedDate
                )
        ) {

            return 0L;
        }


        return ChronoUnit.DAYS.between(
                expectedDate,
                compareDate
        );
    }


    // =========================================================
    // CANCELLED / REJECTED
    // =========================================================

    private boolean isTerminalWithoutCompletion(
            String status
    ) {

        return "CANCELLED".equals(
                status
        )
                || "REJECTED".equals(
                status
        );
    }


    // =========================================================
    // RATE
    // =========================================================

    private BigDecimal calculateRate(

            long numerator,

            long denominator
    ) {

        if (
                denominator <= 0L
        ) {

            return BigDecimal.ZERO;
        }


        return BigDecimal
                .valueOf(
                        numerator
                )

                .multiply(
                        BigDecimal.valueOf(
                                100L
                        )
                )

                .divide(
                        BigDecimal.valueOf(
                                denominator
                        ),
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // FORMAT DELAY DAYS
    // =========================================================

    private String formatDelayDays(
            Long delayDays
    ) {

        if (
                delayDays == null
                        || delayDays <= 0L
        ) {

            return "";
        }


        return " " + delayDays + " ngày";
    }


    // =========================================================
    // VALIDATE DATE RANGE
    // =========================================================

    private void validateDateRange(

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(
                                toDate
                        )
        ) {

            throw new BadRequest(
                    "Thời gian bắt đầu không được sau thời gian kết thúc"
            );
        }
    }


    // =========================================================
    // SAFE LONG
    // =========================================================

    private long safeLong(
            Long value
    ) {

        return value == null
                ? 0L
                : value;
    }
}