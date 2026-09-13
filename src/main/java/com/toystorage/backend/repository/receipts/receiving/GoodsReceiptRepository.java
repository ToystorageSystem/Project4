package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.receipts.InspectionResult;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipts, Long> {

    Optional<GoodsReceipts>
    findByReceiptCode(
            String receiptCode
    );

    Optional<GoodsReceipts>
    findByGoodsReceiptsCode(
            String goodsReceiptsCode
    );


    /*
     * =====================================================
     * NORMAL PAGINATION
     * =====================================================
     */

    Page<GoodsReceipts>
    findByWarehouseIdAndStatus(
            Long warehouseId,
            GoodsReceiptStatus status,
            Pageable pageable
    );


    /*
     * =====================================================
     * OPTIMIZED RECEIVING MONITOR
     * =====================================================
     *
     * Query thẳng DTO.
     *
     * Không:
     * Page<GoodsReceipts>
     *      ↓
     * map(buildMonitorResponse)
     *
     * nữa.
     *
     * Mục tiêu:
     * tránh N+1 khi load monitor list.
     */
    @Query(
            value = """
                    SELECT new com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse(
                        gr.id,
                        gr.receiptCode,
                        CAST(gr.status AS string),
                        gr.warehouse.id,

                      COALESCE(staff.id, receivedStaff.id),
                      COALESCE(staff.name, receivedStaff.name),

                        CAST(COUNT(DISTINCT gri.id) AS integer),

                        CAST(
                            COUNT(
                                DISTINCT CASE
                                    WHEN ri.id IS NOT NULL
                                    THEN gri.id
                                    ELSE NULL
                                END
                            )
                            AS integer
                        ),

                        CAST(
                            COUNT(DISTINCT gri.id)
                            -
                            COUNT(
                                DISTINCT CASE
                                    WHEN ri.id IS NOT NULL
                                    THEN gri.id
                                    ELSE NULL
                                END
                            )
                            AS integer
                        ),

                        CAST(
                            COUNT(
                                DISTINCT CASE
                                    WHEN ri.inspectedResult <> :matchedResult
                                    THEN gri.id
                                    ELSE NULL
                                END
                            )
                            AS integer
                        ),

                        CAST(
                            COALESCE(
                                SUM(DISTINCT gri.expectedQuantity),
                                0
                            )
                            AS integer
                        ),

                        CAST(
                            COALESCE(
                                SUM(
                                    DISTINCT CASE
                                        WHEN ri.id IS NOT NULL
                                        THEN gri.actualQuantity
                                        ELSE 0
                                    END
                                ),
                                0
                            )
                            AS integer
                        ),

                        CAST(
                            COALESCE(
                                SUM(DISTINCT gri.expectedQuantity),
                                0
                            )
                            -
                            COALESCE(
                                SUM(
                                    DISTINCT CASE
                                        WHEN ri.id IS NOT NULL
                                        THEN gri.actualQuantity
                                        ELSE 0
                                    END
                                ),
                                0
                            )
                            AS integer
                        ),

                        CASE
                            WHEN COUNT(DISTINCT gri.id) = 0
                            THEN 0.0

                            ELSE
                                (
                                    COUNT(
                                        DISTINCT CASE
                                            WHEN ri.id IS NOT NULL
                                            THEN gri.id
                                            ELSE NULL
                                        END
                                    )
                                    * 100.0
                                    /
                                    COUNT(DISTINCT gri.id)
                                )
                        END,

                        claim.claimedAt
                    )
                    FROM GoodsReceipts gr

                    LEFT JOIN GoodsReceiptItems gri
                        ON gri.goodsReceipt.id = gr.id

                    LEFT JOIN WarehouseTaskClaim claim
                        ON claim.referenceId = gr.id
                        AND claim.taskType = :taskType
                        AND claim.releasedAt IS NULL
                    LEFT JOIN claim.claimedBy staff
                                                   
                    LEFT JOIN gr.receivedBy receivedStaff
                                                   
                    LEFT JOIN ReceiptInspections ri
                        ON ri.taskClaim.id = claim.id
                        AND ri.product.id = gri.product.id
                    WHERE gr.warehouse.id = :warehouseId
                        AND gr.status = :status
                        AND (
                             :keyword = ''
                             OR LOWER(gr.receiptCode)
                             LIKE CONCAT('%', :keyword, '%')
                             OR LOWER(COALESCE(receivedStaff.name, ''))
                             LIKE CONCAT('%', :keyword, '%')
                    )
                    GROUP BY
                            gr.id,
                            gr.receiptCode,
                            gr.status,
                            gr.warehouse.id,
                            staff.id,
                            staff.name,
                            receivedStaff.id,
                            receivedStaff.name,
                            claim.claimedAt
                    """,

            countQuery = """
        SELECT COUNT(gr.id)
        FROM GoodsReceipts gr

        LEFT JOIN gr.receivedBy receivedStaff

        WHERE gr.warehouse.id = :warehouseId
          AND gr.status = :status
          AND (
                :keyword = ''
                OR LOWER(gr.receiptCode)
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(receivedStaff.name, ''))
                    LIKE CONCAT('%', :keyword, '%')
              )
        """
    )
    Page<ReceivingMonitorResponse>
    findReceivingMonitorPage(
            @Param("warehouseId")
            Long warehouseId,

            @Param("status")
            GoodsReceiptStatus status,

            @Param("taskType")
            WarehouseTaskType taskType,

            @Param("matchedResult")
            InspectionResult matchedResult,

            @Param("keyword")
            String keyword,

            Pageable pageable
    );


    List<GoodsReceipts>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<GoodsReceiptStatus> statuses
    );
}